from __future__ import annotations

import gzip
import io
import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime
from typing import Optional
from xml.etree import ElementTree as ET

import httpx

logger = logging.getLogger(__name__)


@dataclass
class RemoteFile:
    url: str
    name: str
    file_type: str   # "prices" | "promos" | "stores"
    modified: Optional[datetime] = None


@dataclass
class ParsedPrice:
    item_code: str
    item_name: str
    manufacturer_name: str
    unit_qty: str
    quantity: float
    is_weighted: bool
    unit_of_measure: str
    price: float
    unit_measure_price: float
    allow_discount: bool


@dataclass
class ParsedPromo:
    promo_id: str
    description: str
    promo_type: int
    discount_rate: float
    min_qty: int
    max_qty: int
    start_date: Optional[datetime]
    end_date: Optional[datetime]


@dataclass
class ParsedStore:
    store_id: str
    name: str
    address: str
    city: str


class BaseScraper(ABC):
    CHAIN_ID: str
    NAME: str
    DISPLAY_NAME: str
    BASE_URL: str

    def __init__(self):
        self.client = httpx.Client(timeout=60, follow_redirects=True)

    def close(self):
        self.client.close()

    @abstractmethod
    def list_files(self) -> list[RemoteFile]:
        """Return all currently available remote files for this chain."""

    def download_and_decompress(self, url: str) -> bytes:
        resp = self.client.get(url)
        resp.raise_for_status()
        data = resp.content
        if data[:2] == b"\x1f\x8b":
            return gzip.decompress(data)
        if data[:2] == b"PK":
            import zipfile, io
            with zipfile.ZipFile(io.BytesIO(data)) as zf:
                # Return the first XML-like file inside the ZIP
                names = zf.namelist()
                xml_names = [n for n in names if n.lower().endswith(".xml")]
                target = xml_names[0] if xml_names else names[0]
                return zf.read(target)
        return data

    # ------------------------------------------------------------------ #
    # XML parsing helpers — shared across chains that follow the standard  #
    # ------------------------------------------------------------------ #

    @staticmethod
    def _text(el: ET.Element, tag: str, default: str = "") -> str:
        child = el.find(tag)
        return child.text.strip() if child is not None and child.text else default

    @staticmethod
    def _float(el: ET.Element, tag: str, default: float = 0.0) -> float:
        try:
            return float(BaseScraper._text(el, tag, str(default)))
        except ValueError:
            return default

    @staticmethod
    def _int(el: ET.Element, tag: str, default: int = 0) -> int:
        try:
            return int(BaseScraper._text(el, tag, str(default)))
        except ValueError:
            return default

    @staticmethod
    def _dt(el: ET.Element, tag: str) -> Optional[datetime]:
        raw = BaseScraper._text(el, tag)
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y%m%d", "%Y-%m-%d"):
            try:
                return datetime.strptime(raw, fmt)
            except ValueError:
                continue
        return None

    def parse_prices(self, xml_bytes: bytes) -> list[ParsedPrice]:
        root = ET.fromstring(xml_bytes)
        items_el = root.find(".//Items") or root.find(".//Prices")
        if items_el is None:
            return []

        results = []
        for item in items_el:
            try:
                results.append(ParsedPrice(
                    item_code=self._text(item, "ItemCode") or self._text(item, "PriceUpdateDate"),
                    item_name=self._text(item, "ItemName"),
                    manufacturer_name=self._text(item, "ManufacturerName"),
                    unit_qty=self._text(item, "UnitQty"),
                    quantity=self._float(item, "Quantity", 1.0),
                    is_weighted=self._int(item, "bIsWeighted") == 1,
                    unit_of_measure=self._text(item, "UnitOfMeasure"),
                    price=self._float(item, "ItemPrice"),
                    unit_measure_price=self._float(item, "UnitOfMeasurePrice"),
                    allow_discount=self._int(item, "AllowDiscount") == 1,
                ))
            except Exception as exc:
                logger.warning("Skipping malformed price item: %s", exc)
        return results

    def parse_promos(self, xml_bytes: bytes) -> list[ParsedPromo]:
        root = ET.fromstring(xml_bytes)
        promos_el = root.find(".//Promotions")
        if promos_el is None:
            return []

        results = []
        for promo in promos_el:
            try:
                results.append(ParsedPromo(
                    promo_id=self._text(promo, "PromotionId"),
                    description=self._text(promo, "PromotionDescription"),
                    promo_type=self._int(promo, "RewardType"),
                    discount_rate=self._float(promo, "DiscountRate"),
                    min_qty=self._int(promo, "MinQty"),
                    max_qty=self._int(promo, "MaxQty"),
                    start_date=self._dt(promo, "StartDate"),
                    end_date=self._dt(promo, "EndDate"),
                ))
            except Exception as exc:
                logger.warning("Skipping malformed promo: %s", exc)
        return results

    def parse_stores(self, xml_bytes: bytes) -> list[ParsedStore]:
        root = ET.fromstring(xml_bytes)
        # Chains use different element names: Store (most), Branch (Shufersal)
        candidates = list(root.iter("Store")) + list(root.iter("Branch"))
        seen: set[str] = set()
        results = []
        for el in candidates:
            try:
                store_id = self._text(el, "StoreId")
                if not store_id or store_id in seen:
                    continue
                seen.add(store_id)
                results.append(ParsedStore(
                    store_id=store_id,
                    name=self._text(el, "StoreName"),
                    address=self._text(el, "Address"),
                    city=self._text(el, "City"),
                ))
            except Exception as exc:
                logger.warning("Skipping malformed store: %s", exc)
        return results
