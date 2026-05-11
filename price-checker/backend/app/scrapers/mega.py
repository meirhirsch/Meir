"""
Mega / Co-op uses publishedprices.co.il — same HTML directory pattern as Rami Levy.
"""
from __future__ import annotations

import logging
import re

from app.scrapers.base import BaseScraper, RemoteFile
from app.scrapers.rami_levy import _detect_type

logger = logging.getLogger(__name__)


class MegaScraper(BaseScraper):
    CHAIN_ID = "7290055700007"
    NAME = "mega"
    DISPLAY_NAME = "מגה"
    BASE_URL = "http://publishedprices.co.il/cache/files"

    def list_files(self) -> list[RemoteFile]:
        files: list[RemoteFile] = []
        try:
            resp = self.client.get(self.BASE_URL + "/")
            resp.raise_for_status()
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
            logger.error("Mega list_files failed: %s", exc)
        return files
