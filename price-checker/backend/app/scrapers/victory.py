"""
Victory (מטריקס) publishes an XML index at:
  http://matrixcatalog.co.il/NBCompetitionData.aspx
The index lists download URLs for price / promo / store files.
"""
from __future__ import annotations

import logging
from xml.etree import ElementTree as ET

from app.scrapers.base import BaseScraper, RemoteFile

logger = logging.getLogger(__name__)

FILE_TYPE_MAP = {
    "pricefull": "prices",
    "price": "prices",
    "promofull": "promos",
    "promo": "promos",
    "storesfull": "stores",
    "stores": "stores",
}


def _detect_type(name: str) -> str:
    lower = name.lower()
    for key, val in FILE_TYPE_MAP.items():
        if key in lower:
            return val
    return "prices"


class VictoryScraper(BaseScraper):
    CHAIN_ID = "7290696200003"
    NAME = "victory"
    DISPLAY_NAME = "ויקטורי"
    BASE_URL = "http://matrixcatalog.co.il"
    INDEX_URL = "http://matrixcatalog.co.il/NBCompetitionData.aspx"

    def list_files(self) -> list[RemoteFile]:
        files: list[RemoteFile] = []
        try:
            resp = self.client.get(self.INDEX_URL)
            resp.raise_for_status()
            root = ET.fromstring(resp.content)
            for file_el in root.iter("File"):
                url_el = file_el.find("URL") or file_el.find("FileNm")
                if url_el is None or not url_el.text:
                    continue
                url = url_el.text.strip()
                name = url.split("/")[-1]
                files.append(RemoteFile(
                    url=url,
                    name=name,
                    file_type=_detect_type(name),
                ))
        except Exception as exc:
            logger.error("Victory list_files failed: %s", exc)
        return files
