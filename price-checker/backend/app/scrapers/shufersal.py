"""
Shufersal publishes a JSON index at:
  https://prices.shufersal.co.il/FileObject/UpdateCategory?catID=<N>&storeId=0&pagingSize=10000&pagingOffset=0
catID=2 → PriceFull  catID=4 → PromoFull  catID=6 → StoresFull
"""
from __future__ import annotations

import logging
from datetime import datetime

from app.scrapers.base import BaseScraper, RemoteFile

logger = logging.getLogger(__name__)

CATEGORIES = {
    2: "prices",
    4: "promos",
    6: "stores",
}


class ShufersalScraper(BaseScraper):
    CHAIN_ID = "7290058140886"
    NAME = "shufersal"
    DISPLAY_NAME = "שופרסל"
    BASE_URL = "https://prices.shufersal.co.il"

    def list_files(self) -> list[RemoteFile]:
        files: list[RemoteFile] = []
        for cat_id, file_type in CATEGORIES.items():
            url = (
                f"{self.BASE_URL}/FileObject/UpdateCategory"
                f"?catID={cat_id}&storeId=0&pagingSize=10000&pagingOffset=0"
            )
            try:
                resp = self.client.get(url)
                resp.raise_for_status()
                data = resp.json()
                for entry in data.get("Data", []):
                    file_url = entry.get("FileNm", "")
                    file_name = file_url.split("/")[-1]
                    modified_raw = entry.get("FileVldDt", "")
                    modified = None
                    if modified_raw:
                        try:
                            modified = datetime.strptime(modified_raw[:19], "%Y-%m-%dT%H:%M:%S")
                        except ValueError:
                            pass
                    if file_url:
                        files.append(RemoteFile(
                            url=file_url,
                            name=file_name,
                            file_type=file_type,
                            modified=modified,
                        ))
            except Exception as exc:
                logger.error("Shufersal list_files (catID=%d) failed: %s", cat_id, exc)
        return files
