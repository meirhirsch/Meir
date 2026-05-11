"""
Rami Levy publishes files via publishedprices.co.il.
Directory listing is available as HTML — we parse href links to .gz files.
"""
from __future__ import annotations

import logging
import re
from datetime import datetime

from app.scrapers.base import BaseScraper, RemoteFile

logger = logging.getLogger(__name__)

FILE_TYPE_MAP = {
    "PriceFull": "prices",
    "Price": "prices",
    "PromoFull": "promos",
    "Promo": "promos",
    "StoresFull": "stores",
    "Stores": "stores",
}


def _detect_type(name: str) -> str:
    for key, val in FILE_TYPE_MAP.items():
        if key.lower() in name.lower():
            return val
    return "prices"


class RamiLevyScraper(BaseScraper):
    CHAIN_ID = "7290058108879"
    NAME = "rami_levy"
    DISPLAY_NAME = "רמי לוי"
    BASE_URL = "http://url.retail.publishedprices.co.il/cache/files"

    def list_files(self) -> list[RemoteFile]:
        files: list[RemoteFile] = []
        try:
            resp = self.client.get(self.BASE_URL + "/")
            resp.raise_for_status()
            # Parse gz links from directory listing HTML
            hrefs = re.findall(r'href="([^"]+\.gz)"', resp.text)
            for href in hrefs:
                name = href.split("/")[-1]
                full_url = (
                    href if href.startswith("http") else f"{self.BASE_URL}/{name}"
                )
                files.append(RemoteFile(
                    url=full_url,
                    name=name,
                    file_type=_detect_type(name),
                ))
        except Exception as exc:
            logger.error("Rami Levy list_files failed: %s", exc)
        return files
