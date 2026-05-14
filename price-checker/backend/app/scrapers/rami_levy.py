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
            import time
            ts = int(time.time() * 1000)
            json_headers = {
                "Accept": "application/json, text/javascript, */*",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": f"{BASE_URL}/file/d",
            }

            # Cerberus known JSON endpoints (tried in order, stop at first success)
            endpoints = [
                f"/file/json?__swhg={ts}",
                "/file/json",
                f"/file/d?__swhg={ts}",
                "/file/d?format=json",
                "/file/d?json=1",
            ]

            for endpoint in endpoints:
                try:
                    r = self.client.get(f"{BASE_URL}{endpoint}", headers=json_headers)
                    if "/login" in str(r.url):
                        logger.error("Rami Levy: session expired at %s", endpoint)
                        return []
                    if r.status_code != 200:
                        logger.debug("Rami Levy %s → %d", endpoint, r.status_code)
                        continue

                    content_type = r.headers.get("content-type", "")
                    logger.debug("Rami Levy %s → %d ct=%s preview=%s",
                                 endpoint, r.status_code, content_type, r.text[:300])

                    if "json" in content_type:
                        data = r.json()
                    else:
                        try:
                            data = r.json()
                        except Exception:
                            # Plain HTML — scan for .gz names
                            names = re.findall(r'[\w\.\-]+\.gz', r.text)
                            for name in set(names):
                                if any(k in name.lower() for k in ["price", "promo", "store"]):
                                    files.append(RemoteFile(
                                        url=f"{BASE_URL}/file/d/{name}",
                                        name=name,
                                        file_type=_detect_type(name),
                                    ))
                            if files:
                                logger.info("Rami Levy: found %d .gz names in %s HTML", len(files), endpoint)
                                break
                            continue

                    # Parse JSON response
                    if isinstance(data, list):
                        entries = data
                    elif isinstance(data, dict):
                        entries = data.get("data", data.get("files", data.get("Data", [])))
                    else:
                        entries = []

                    for entry in entries:
                        if isinstance(entry, str):
                            name = entry
                        else:
                            name = (entry.get("name") or entry.get("fileName")
                                    or entry.get("FileNm") or entry.get("FileName") or "")
                        if name and name.endswith(".gz"):
                            files.append(RemoteFile(
                                url=f"{BASE_URL}/file/d/{name}",
                                name=name,
                                file_type=_detect_type(name),
                            ))

                    if files:
                        logger.info("Rami Levy: found %d files via %s", len(files), endpoint)
                        break
                    else:
                        logger.debug("Rami Levy %s returned 0 entries", endpoint)

                except Exception as exc:
                    logger.debug("Rami Levy %s error: %s", endpoint, exc)

            if not files:
                logger.warning("Rami Levy: all endpoints returned 0 files")

            logger.info("Rami Levy: total %d files", len(files))
        except Exception as exc:
            logger.error("Rami Levy: file listing failed: %s", exc)

        return files

    def download_and_decompress(self, url: str) -> bytes:
        if not self._logged_in and not self._login():
            raise RuntimeError("Not logged in to Cerberus")
        return super().download_and_decompress(url)
