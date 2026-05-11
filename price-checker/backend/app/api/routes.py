from __future__ import annotations

from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, BackgroundTasks
from pydantic import BaseModel
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Chain, Product, Price, Promotion, SyncLog
from app.scheduler import trigger_now

router = APIRouter()


# ── Pydantic response schemas ──────────────────────────────────────────────── #

class ChainOut(BaseModel):
    id: int
    chain_id: str
    name: str
    display_name: str
    last_synced: Optional[datetime]

    class Config:
        from_attributes = True


class PriceOut(BaseModel):
    chain_id: int
    chain_name: str
    chain_display_name: str
    price: float
    unit_measure_price: Optional[float]
    allow_discount: bool
    updated_at: Optional[datetime]


class ProductOut(BaseModel):
    id: int
    item_code: str
    name: str
    manufacturer_name: Optional[str]
    unit_qty: Optional[str]
    quantity: Optional[float]
    is_weighted: bool
    unit_of_measure: Optional[str]
    prices: list[PriceOut] = []

    class Config:
        from_attributes = True


class ProductSummary(BaseModel):
    id: int
    item_code: str
    name: str
    manufacturer_name: Optional[str]
    min_price: Optional[float]
    max_price: Optional[float]
    num_chains: int


class SyncLogOut(BaseModel):
    id: int
    chain_id: int
    file_name: str
    file_type: Optional[str]
    downloaded_at: Optional[datetime]
    records_count: int
    status: str
    error_message: Optional[str]

    class Config:
        from_attributes = True


class SyncStatusOut(BaseModel):
    chains: list[ChainOut]
    recent_logs: list[SyncLogOut]


# ── Endpoints ─────────────────────────────────────────────────────────────── #

@router.get("/chains", response_model=list[ChainOut])
def list_chains(db: Session = Depends(get_db)):
    return db.query(Chain).all()


@router.get("/products", response_model=list[ProductSummary])
def list_products(
    q: Optional[str] = Query(None, description="Search by name or barcode"),
    chain_id: Optional[int] = Query(None),
    skip: int = 0,
    limit: int = Query(50, le=200),
    db: Session = Depends(get_db),
):
    query = db.query(
        Product,
        func.min(Price.price).label("min_price"),
        func.max(Price.price).label("max_price"),
        func.count(Price.chain_id.distinct()).label("num_chains"),
    ).outerjoin(Price, Price.product_id == Product.id)

    if chain_id:
        query = query.filter(Price.chain_id == chain_id)

    if q:
        like = f"%{q}%"
        query = query.filter(
            Product.name.ilike(like) | Product.item_code.ilike(like)
        )

    query = query.group_by(Product.id).offset(skip).limit(limit)
    rows = query.all()

    return [
        ProductSummary(
            id=prod.id,
            item_code=prod.item_code,
            name=prod.name,
            manufacturer_name=prod.manufacturer_name,
            min_price=min_price,
            max_price=max_price,
            num_chains=num_chains,
        )
        for prod, min_price, max_price, num_chains in rows
    ]


@router.get("/products/{item_code}", response_model=ProductOut)
def get_product(item_code: str, db: Session = Depends(get_db)):
    product = db.query(Product).filter_by(item_code=item_code).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    price_rows = (
        db.query(Price).filter_by(product_id=product.id).all()
    )
    prices_out = [
        PriceOut(
            chain_id=p.chain_id,
            chain_name=p.chain.name,
            chain_display_name=p.chain.display_name,
            price=p.price,
            unit_measure_price=p.unit_measure_price,
            allow_discount=p.allow_discount,
            updated_at=p.updated_at,
        )
        for p in price_rows
    ]

    return ProductOut(
        id=product.id,
        item_code=product.item_code,
        name=product.name,
        manufacturer_name=product.manufacturer_name,
        unit_qty=product.unit_qty,
        quantity=product.quantity,
        is_weighted=product.is_weighted,
        unit_of_measure=product.unit_of_measure,
        prices=sorted(prices_out, key=lambda x: x.price),
    )


@router.get("/sync/status", response_model=SyncStatusOut)
def sync_status(db: Session = Depends(get_db)):
    chains = db.query(Chain).all()
    recent_logs = (
        db.query(SyncLog)
        .order_by(SyncLog.downloaded_at.desc())
        .limit(50)
        .all()
    )
    return SyncStatusOut(chains=chains, recent_logs=recent_logs)


@router.post("/sync/trigger")
def trigger_sync(background_tasks: BackgroundTasks):
    background_tasks.add_task(trigger_now)
    return {"message": "Sync triggered in background"}
