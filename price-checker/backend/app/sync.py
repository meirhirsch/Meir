"""Sync engine: for each chain, download new GZ files and upsert into DB."""
from __future__ import annotations

import logging
from datetime import datetime
from typing import Type

from sqlalchemy.orm import Session

from app.database import SessionLocal
from app.models import Chain, Store, Product, Price, Promotion, SyncLog
from app.scrapers import ALL_SCRAPERS
from app.scrapers.base import BaseScraper, ParsedPrice, ParsedPromo, ParsedStore

logger = logging.getLogger(__name__)


def _get_or_create_chain(db: Session, scraper: BaseScraper) -> Chain:
    chain = db.query(Chain).filter_by(chain_id=scraper.CHAIN_ID).first()
    if not chain:
        chain = Chain(
            chain_id=scraper.CHAIN_ID,
            name=scraper.NAME,
            display_name=scraper.DISPLAY_NAME,
            base_url=scraper.BASE_URL,
        )
        db.add(chain)
        db.flush()
    return chain


def _get_or_create_store(db: Session, chain: Chain, store_id: str) -> Store:
    store = db.query(Store).filter_by(chain_id=chain.id, store_id=store_id).first()
    if not store:
        store = Store(chain_id=chain.id, store_id=store_id)
        db.add(store)
        db.flush()
    return store


def _already_synced(db: Session, chain: Chain, file_name: str) -> bool:
    return (
        db.query(SyncLog)
        .filter_by(chain_id=chain.id, file_name=file_name, status="ok")
        .first()
        is not None
    )


def _log_sync(db: Session, chain: Chain, file_name: str, file_url: str,
              file_type: str, count: int, error: str | None = None):
    existing = db.query(SyncLog).filter_by(chain_id=chain.id, file_name=file_name).first()
    if existing:
        existing.downloaded_at = datetime.utcnow()
        existing.records_count = count
        existing.status = "error" if error else "ok"
        existing.error_message = error
    else:
        db.add(SyncLog(
            chain_id=chain.id,
            file_name=file_name,
            file_url=file_url,
            file_type=file_type,
            records_count=count,
            status="error" if error else "ok",
            error_message=error,
        ))


def _upsert_prices(db: Session, chain: Chain, prices: list[ParsedPrice]) -> int:
    count = 0
    for p in prices:
        product = db.query(Product).filter_by(item_code=p.item_code).first()
        if not product:
            product = Product(
                item_code=p.item_code,
                name=p.item_name,
                manufacturer_name=p.manufacturer_name,
                unit_qty=p.unit_qty,
                quantity=p.quantity,
                is_weighted=p.is_weighted,
                unit_of_measure=p.unit_of_measure,
            )
            db.add(product)
            db.flush()
        else:
            product.name = p.item_name
            product.manufacturer_name = p.manufacturer_name

        price_row = (
            db.query(Price)
            .filter_by(product_id=product.id, chain_id=chain.id, store_id=None)
            .first()
        )
        if not price_row:
            db.add(Price(
                product_id=product.id,
                chain_id=chain.id,
                price=p.price,
                unit_measure_price=p.unit_measure_price,
                allow_discount=p.allow_discount,
                updated_at=datetime.utcnow(),
            ))
        else:
            price_row.price = p.price
            price_row.unit_measure_price = p.unit_measure_price
            price_row.allow_discount = p.allow_discount
            price_row.updated_at = datetime.utcnow()
        count += 1
    return count


def _upsert_stores(db: Session, chain: Chain, stores: list[ParsedStore]) -> int:
    count = 0
    for s in stores:
        store = db.query(Store).filter_by(chain_id=chain.id, store_id=s.store_id).first()
        if not store:
            store = Store(chain_id=chain.id, store_id=s.store_id)
            db.add(store)
        store.name = s.name
        store.address = s.address
        store.city = s.city
        count += 1
    return count


def sync_chain(scraper_class: Type[BaseScraper]) -> dict:
    scraper = scraper_class()
    db = SessionLocal()
    result = {"chain": scraper.NAME, "new_files": 0, "records": 0, "errors": 0}
    try:
        chain = _get_or_create_chain(db, scraper)
        remote_files = scraper.list_files()
        logger.info("%s: found %d remote files", scraper.DISPLAY_NAME, len(remote_files))

        for rf in remote_files:
            if _already_synced(db, chain, rf.name):
                continue

            try:
                raw = scraper.download_and_decompress(rf.url)
                count = 0

                if rf.file_type == "prices":
                    parsed = scraper.parse_prices(raw)
                    count = _upsert_prices(db, chain, parsed)
                elif rf.file_type == "promos":
                    # Promo upsert: simple insert-or-skip for now
                    parsed = scraper.parse_promos(raw)
                    count = len(parsed)
                elif rf.file_type == "stores":
                    parsed = scraper.parse_stores(raw)
                    count = _upsert_stores(db, chain, parsed)

                _log_sync(db, chain, rf.name, rf.url, rf.file_type, count)
                db.commit()

                result["new_files"] += 1
                result["records"] += count
                logger.info("%s: processed %s (%d records)", scraper.DISPLAY_NAME, rf.name, count)

            except Exception as exc:
                db.rollback()
                _log_sync(db, chain, rf.name, rf.url, rf.file_type, 0, str(exc))
                db.commit()
                result["errors"] += 1
                logger.error("%s: error processing %s — %s", scraper.DISPLAY_NAME, rf.name, exc)

        chain.last_synced = datetime.utcnow()
        db.commit()
    finally:
        scraper.close()
        db.close()

    return result


def sync_all() -> list[dict]:
    results = []
    for scraper_class in ALL_SCRAPERS:
        try:
            res = sync_chain(scraper_class)
            results.append(res)
        except Exception as exc:
            logger.error("Fatal error syncing %s: %s", scraper_class.NAME, exc)
            results.append({"chain": scraper_class.NAME, "error": str(exc)})
    return results
