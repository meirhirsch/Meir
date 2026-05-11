from datetime import datetime
from sqlalchemy import (
    Column, Integer, String, Float, Boolean,
    DateTime, ForeignKey, UniqueConstraint, Text
)
from sqlalchemy.orm import relationship
from app.database import Base


class Chain(Base):
    __tablename__ = "chains"

    id = Column(Integer, primary_key=True)
    chain_id = Column(String, unique=True, nullable=False)  # e.g. "7290058140886"
    name = Column(String, nullable=False)                   # e.g. "shufersal"
    display_name = Column(String, nullable=False)            # e.g. "שופרסל"
    base_url = Column(String, nullable=False)
    last_synced = Column(DateTime, nullable=True)

    stores = relationship("Store", back_populates="chain")
    prices = relationship("Price", back_populates="chain")
    promotions = relationship("Promotion", back_populates="chain")
    sync_logs = relationship("SyncLog", back_populates="chain")


class Store(Base):
    __tablename__ = "stores"

    id = Column(Integer, primary_key=True)
    chain_id = Column(Integer, ForeignKey("chains.id"), nullable=False)
    store_id = Column(String, nullable=False)   # chain's internal store ID
    name = Column(String)
    address = Column(String)
    city = Column(String)

    __table_args__ = (UniqueConstraint("chain_id", "store_id"),)

    chain = relationship("Chain", back_populates="stores")
    prices = relationship("Price", back_populates="store")


class Product(Base):
    __tablename__ = "products"

    id = Column(Integer, primary_key=True)
    item_code = Column(String, unique=True, nullable=False)  # barcode / internal code
    name = Column(String, nullable=False)
    manufacturer_name = Column(String)
    unit_qty = Column(String)          # e.g. "ק\"ג", "יחידה"
    quantity = Column(Float)
    is_weighted = Column(Boolean, default=False)
    unit_of_measure = Column(String)

    prices = relationship("Price", back_populates="product")


class Price(Base):
    __tablename__ = "prices"

    id = Column(Integer, primary_key=True)
    product_id = Column(Integer, ForeignKey("products.id"), nullable=False)
    chain_id = Column(Integer, ForeignKey("chains.id"), nullable=False)
    store_id = Column(Integer, ForeignKey("stores.id"), nullable=True)
    price = Column(Float, nullable=False)
    unit_measure_price = Column(Float)
    allow_discount = Column(Boolean, default=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    __table_args__ = (UniqueConstraint("product_id", "chain_id", "store_id"),)

    product = relationship("Product", back_populates="prices")
    chain = relationship("Chain", back_populates="prices")
    store = relationship("Store", back_populates="prices")


class Promotion(Base):
    __tablename__ = "promotions"

    id = Column(Integer, primary_key=True)
    chain_id = Column(Integer, ForeignKey("chains.id"), nullable=False)
    store_id = Column(Integer, ForeignKey("stores.id"), nullable=True)
    promo_id = Column(String)
    description = Column(Text)
    promo_type = Column(Integer)        # chain's internal promo type code
    discount_rate = Column(Float)
    min_qty = Column(Integer)
    max_qty = Column(Integer)
    start_date = Column(DateTime)
    end_date = Column(DateTime)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    chain = relationship("Chain", back_populates="promotions")


class SyncLog(Base):
    __tablename__ = "sync_logs"

    id = Column(Integer, primary_key=True)
    chain_id = Column(Integer, ForeignKey("chains.id"), nullable=False)
    file_name = Column(String, nullable=False)
    file_url = Column(String)
    file_type = Column(String)   # "prices" | "promos" | "stores"
    downloaded_at = Column(DateTime, default=datetime.utcnow)
    records_count = Column(Integer, default=0)
    status = Column(String, default="ok")   # "ok" | "error"
    error_message = Column(Text)

    __table_args__ = (UniqueConstraint("chain_id", "file_name"),)

    chain = relationship("Chain", back_populates="sync_logs")
