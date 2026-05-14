"""
Shufersal publishes price files on Azure Blob Storage.
The listing is available via their JSON API:
  https://prices.shufersal.co.il/FileObject/UpdateCategory?catID=<N>&storeId=0&pagingSize=10000&pagingOffset=0
catID=2 → PriceFull  catID=4 → PromoFull  catID=6 → StoresFull

Files are served with expiring SAS tokens so URLs are fetched fresh each sync.
"""
from __future__ import annotations

import logging
import re
from datetime import datetime

from app.scrapers.base import BaseScraper, RemoteFile

logger = logging.getLogger(__name__)

CATEGORIES = {
    2: "prices",
    4: "promos",
    6: "stores",
}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36",
    "Accept": "application/json, text/html, */*",
    "Referer": "https://prices.shufersal.co.il/",
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
    lower = name.lower().split("?")[0]  # strip SAS query params
    for key, val in FILE_TYPE_MAP.items():
        if key in lower:
            return val
    return "prices"


def _strip_sas(url: str) -> str:
    """Return the base filename without SAS token for use as a stable key."""
    return url.split("?")[0].split("/")[-1]


class ShufersalScraper(BaseScraper):
    CHAIN_ID = "7290058140886"
    NAME = "shufersal"
    DISPLAY_NAME = "שופרסל"
    BASE_URL = "https://prices.shufersal.co.il"

    def list_files(self) -> list[RemoteFile]:
        files: list[RemoteFile] = []

        # Try JSON API first
        for cat_id, file_type in CATEGORIES.items():
            url = (
                f"{self.BASE_URL}/FileObject/UpdateCategory"
                f"?catID={cat_id}&storeId=0&pagingSize=10000&pagingOffset=0"
            )
            try:
                resp = self.client.get(url, headers=HEADERS)
                resp.raise_for_status()
                data = resp.json()
                for entry in data.get("Data", []):
                    file_url = entry.get("FileNm", "")
                    if not file_url:
                        continue
                    file_name = _strip_sas(file_url)
                    modified_raw = entry.get("FileVldDt", "")
                    modified = None
                    if modified_raw:
                        try:
                            modified = datetime.strptime(modified_raw[:19], "%Y-%m-%dT%H:%M:%S")
                        except ValueError:
                            pass
                    files.append(RemoteFile(
                        url=file_url,
                        name=file_name,
                        file_type=file_type,
                        modified=modified,
                    ))
                logger.info("Shufersal API catID=%d: %d files", cat_id, len(files))
            except Exception as exc:
                logger.warning("Shufersal JSON API (catID=%d) failed: %s — trying HTML fallback", cat_id, exc)

        # HTML fallback: parse blob.core.windows.net links directly from the page
        if not files:
            try:
                resp = self.client.get(self.BASE_URL, headers=HEADERS)
                resp.raise_for_status()
                urls = re.findall(r'https://[^"\']+\.gz(?:\?[^"\']*)?', resp.text)
                for file_url in urls:
                    file_name = _strip_sas(file_url)
                    files.append(RemoteFile(
                        url=file_url,
                        name=file_name,
                        file_type=_detect_type(file_name),
                    ))
                logger.info("Shufersal HTML fallback: %d files", len(files))
            except Exception as exc:
                logger.error("Shufersal HTML fallback failed: %s", exc)

        return files
