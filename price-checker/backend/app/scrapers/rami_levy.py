"""
Rami Levy uses the Cerberus price publishing system at:
  https://url.publishedprices.co.il/login
Username: RamiLevi  Password: (empty)

Flow:
  1. POST /login/user  → get session cookie
  2. GET  /file/d      → JSON list of available files
  3. GET  /file/d/{filename} → download .gz file
"""
from __future__ import annotations

import logging
import re
from datetime import datetime

import httpx

from app.scrapers.base import BaseScraper, RemoteFile

logger = logging.getLogger(__name__)

BASE_URL = "https://url.publishedprices.co.il"
USERNAME = "RamiLevi"
PASSWORD = ""

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


class RamiLevyScraper(BaseScraper):
    CHAIN_ID = "7290058108879"
    NAME = "rami_levy"
    DISPLAY_NAME = "רמי לוי"
    BASE_URL = BASE_URL

    def __init__(self):
        # Use a cookie-aware client so the session persists across requests
        self.client = httpx.Client(
            timeout=60,
            follow_redirects=True,
            verify=False,  # Cerberus uses a self-signed/corporate cert on Windows
            headers={
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36",
            },
        )
        self._logged_in = False

    def _login(self) -> bool:
        try:
            # Step 1: GET login page — pick up session cookie + extract CSRF token
            login_page = self.client.get(f"{BASE_URL}/login")
            csrf = re.search(r'name="csrftoken"\s+content="([^"]+)"', login_page.text)
            if not csrf:
                csrf = re.search(r'csrftoken["\s:]+([A-Za-z0-9_\-]{20,})', login_page.text)
            csrf_token = csrf.group(1) if csrf else ""
            logger.debug("Rami Levy CSRF token: %s", csrf_token)

            # Step 2: POST with CSRF token
            resp = self.client.post(
                f"{BASE_URL}/login/user",
                data={"username": USERNAME, "password": PASSWORD, "csrftoken": csrf_token},
                headers={"Content-Type": "application/x-www-form-urlencoded"},
            )
            logger.debug("Rami Levy POST → %s cookies=%s", resp.status_code, dict(self.client.cookies))

            # Step 3: Verify session works — check URL and page content
            check = self.client.get(f"{BASE_URL}/file/d")
            final_url = str(check.url)
            if "/login" not in final_url and "login" not in check.text[:200].lower():
                self._logged_in = True
                logger.info("Rami Levy: logged in successfully (url=%s, content_len=%d)",
                            final_url, len(check.text))
                return True

            logger.error("Rami Levy: login failed — url=%s, page_preview=%s",
                         final_url, check.text[:300])
            return False
        except Exception as exc:
            logger.error("Rami Levy: login failed: %s", exc)
            return False

    def list_files(self) -> list[RemoteFile]:
        if not self._logged_in and not self._login():
            return []

        files: list[RemoteFile] = []
        try:
            # Fetch the SPA shell — it contains the CSRF token for subsequent POSTs
            spa_resp = self.client.get(f"{BASE_URL}/file/d")
            spa_html = spa_resp.text

            # Extract CSRF token from the page meta tag
            csrf_match = re.search(r'name="csrftoken"\s+content="([^"]+)"', spa_html)
            if not csrf_match:
                csrf_match = re.search(r'csrftoken["\s:]+([A-Za-z0-9_\-]{20,})', spa_html)
            page_csrf = csrf_match.group(1) if csrf_match else ""
            logger.debug("Rami Levy page CSRF token: %s", page_csrf)

            # /file/json/dir is the DataTables server-side endpoint for file listing
            # It requires POST + CSRF token
            r = self.client.post(
                f"{BASE_URL}/file/json/dir",
                data={
                    "sEcho": "1",
                    "iDisplayStart": "0",
                    "iDisplayLength": "1000",
                    "csrftoken": page_csrf,
                },
                headers={
                    "Accept": "application/json, text/javascript, */*; q=0.01",
                    "X-Requested-With": "XMLHttpRequest",
                    "Referer": f"{BASE_URL}/file/d",
                    "Content-Type": "application/x-www-form-urlencoded",
                },
            )
            logger.debug("Rami Levy /file/json/dir → %d ct=%s body=%s",
                         r.status_code, r.headers.get("content-type", ""), r.text[:400])

            if r.status_code == 200:
                data = r.json()
                aa_data = data.get("aaData", [])
                logger.info("Rami Levy /file/json/dir: %d entries, error=%s",
                            len(aa_data), data.get("error", "none"))
                for entry in aa_data:
                    # DataTables rows may be lists or dicts
                    if isinstance(entry, list):
                        # Typically: [name_html, size, date, type, ...]
                        # Extract filename from first element (may contain HTML)
                        name_raw = str(entry[0]) if entry else ""
                        name_match = re.search(r'[\w\.\-]+\.gz', name_raw)
                        name = name_match.group(0) if name_match else ""
                    elif isinstance(entry, dict):
                        name = (entry.get("name") or entry.get("FileName")
                                or entry.get("fileName") or "")
                        if not name.endswith(".gz"):
                            # Try extracting from HTML value
                            name_match = re.search(r'[\w\.\-]+\.gz', str(next(iter(entry.values()), "")))
                            name = name_match.group(0) if name_match else ""
                    else:
                        name = ""
                    if name and name.endswith(".gz"):
                        files.append(RemoteFile(
                            url=f"{BASE_URL}/file/d/{name}",
                            name=name,
                            file_type=_detect_type(name),
                        ))

            if files:
                logger.info("Rami Levy: found %d files via /file/json/dir", len(files))
            else:
                logger.warning("Rami Levy: /file/json/dir returned 0 files")

        except Exception as exc:
            logger.error("Rami Levy: file listing failed: %s", exc)

        logger.info("Rami Levy: total %d files", len(files))
        return files

    def download_and_decompress(self, url: str) -> bytes:
        if not self._logged_in and not self._login():
            raise RuntimeError("Not logged in to Cerberus")
        return super().download_and_decompress(url)
