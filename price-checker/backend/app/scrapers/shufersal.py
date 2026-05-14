"""
Shufersal publishes price files on Azure Blob Storage.
The listing is available via their JSON API:
  https://prices.shufersal.co.il/FileObject/UpdateCategory?catID=<N>&storeId=0&pagingSize=10000&pagingOffset=0

Known catIDs:
  1 → PriceFull   2 → Price   3 → PromoFull   4 → Promo   6 → StoresFull

Files are served with expiring SAS tokens so URLs are fetched fresh each sync.
"""
from __future__ import annotations

import html
import logging
import re
from datetime import datetime

from app.scrapers.base import BaseScraper, RemoteFile

logger = logging.getLogger(__name__)

# Try all known catIDs — PriceFull (1) gives the complete product catalog
CATEGORIES = {
    1: "prices",   # PriceFull
    2: "prices",   # Price (delta per store)
    3: "promos",   # PromoFull
    4: "promos",   # Promo
    6: "stores",   # StoresFull
}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36",
    "Accept": "application/json, text/javascript, */*",
    "Referer": "https://prices.shufersal.co.il/",
    "X-Requested-With": "XMLHttpRequest",
}

FILE_TYPE_MAP = {
    "pricefull": "prices",
    "price": "prices",
    "promofull": "promos",
    "promo": "promos",
    "storesfull": "stores",
    "stores": "stores",
}


def _detect_type(name: str) -> str:
    lower = name.lower().split("?")[0]
    for key, val in FILE_TYPE_MAP.items():
        if key in lower:
            return val
    return "prices"


def _strip_sas(url: str) -> str:
    """Return the base filename without SAS token — used as a stable dedup key."""
    return url.split("?")[0].split("/")[-1]


class ShufersalScraper(BaseScraper):
    CHAIN_ID = "7290058140886"
    NAME = "shufersal"
    DISPLAY_NAME = "שופרסל"
    BASE_URL = "https://prices.shufersal.co.il"

    def list_files(self) -> list[RemoteFile]:
        files: list[RemoteFile] = []
        seen_names: set[str] = set()

        def add(file_url: str, file_type: str, modified=None):
            file_url = html.unescape(file_url)
            name = _strip_sas(file_url)
            if name not in seen_names:
                seen_names.add(name)
                files.append(RemoteFile(url=file_url, name=name, file_type=file_type, modified=modified))

        # ── Try JSON API for each category ───────────────────────────────
        for cat_id, file_type in CATEGORIES.items():
            import time
            ts = int(time.time() * 1000)
            url = (
                f"{self.BASE_URL}/FileObject/UpdateCategory"
                f"?catID={cat_id}&storeId=0&pagingSize=10000&pagingOffset=0&__swhg={ts}"
            )
            try:
                resp = self.client.get(url, headers=HEADERS)
                resp.raise_for_status()
                logger.debug("Shufersal catID=%d response preview: %s", cat_id, resp.text[:300])
                data = resp.json()
                before = len(files)
                for entry in data.get("Data", []):
                    file_url = entry.get("FileNm", "")
                    if not file_url:
                        continue
                    modified_raw = entry.get("FileVldDt", "")
                    modified = None
                    if modified_raw:
                        try:
                            modified = datetime.strptime(modified_raw[:19], "%Y-%m-%dT%H:%M:%S")
                        except ValueError:
                            pass
                    add(file_url, file_type, modified)
                logger.info("Shufersal API catID=%d: +%d files", cat_id, len(files) - before)
            except Exception as exc:
                logger.warning("Shufersal JSON API catID=%d failed: %s", cat_id, exc)

        # ── HTML fallback: scrape all .gz links from the main page ────────
        if not files:
            try:
                resp = self.client.get(self.BASE_URL, headers={**HEADERS, "Accept": "text/html"})
                resp.raise_for_status()
                for file_url in re.findall(r'https://[^"\'<>\s]+\.gz(?:\?[^"\'<>\s]*)?', resp.text):
                    add(file_url, _detect_type(file_url))
                logger.info("Shufersal HTML fallback: %d files", len(files))
            except Exception as exc:
                logger.error("Shufersal HTML fallback failed: %s", exc)

        logger.info("Shufersal total: %d unique files", len(files))
        return files
