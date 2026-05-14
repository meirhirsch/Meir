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
            # Step 1: GET login page to pick up session cookie + find form fields
            login_page = self.client.get(f"{BASE_URL}/login")
            logger.debug("Rami Levy login page: %s", login_page.text[:500])

            # Step 2: Try all known field-name variants for the username
            for user_field in ["username", "user", "email", "UserName"]:
                resp = self.client.post(
                    f"{BASE_URL}/login/user",
                    data={user_field: USERNAME, "password": PASSWORD},
                    headers={"Content-Type": "application/x-www-form-urlencoded"},
                )
                logger.debug(
                    "Rami Levy POST %s=%s → %s cookies=%s body=%s",
                    user_field, USERNAME, resp.status_code,
                    dict(self.client.cookies), resp.text[:300],
                )
                # Step 3: Check if /file/d is accessible
                check = self.client.get(f"{BASE_URL}/file/d")
                if "/login" not in str(check.url):
                    self._logged_in = True
                    logger.info("Rami Levy: logged in (field=%s)", user_field)
                    return True
                logger.warning("Rami Levy: field=%s didn't work, trying next", user_field)

            logger.error("Rami Levy: all login attempts failed")
            return False
        except Exception as exc:
            logger.error("Rami Levy: login failed: %s", exc)
            return False

    def list_files(self) -> list[RemoteFile]:
        if not self._logged_in and not self._login():
            return []

        files: list[RemoteFile] = []
        try:
            resp = self.client.get(f"{BASE_URL}/file/d")
            if "/login" in str(resp.url):
                logger.error("Rami Levy: session expired, re-logging in")
                self._logged_in = False
                if not self._login():
                    return []
                resp = self.client.get(f"{BASE_URL}/file/d")
            resp.raise_for_status()

            # Response may be JSON or HTML directory listing
            try:
                data = resp.json()
                entries = data if isinstance(data, list) else data.get("files", [])
                for entry in entries:
                    name = entry.get("name", "") or entry.get("fileName", "")
                    if not name:
                        continue
                    files.append(RemoteFile(
                        url=f"{BASE_URL}/file/d/{name}",
                        name=name,
                        file_type=_detect_type(name),
                    ))
            except Exception:
                # Fallback: parse HTML directory listing for .gz links
                hrefs = re.findall(r'href="([^"]*\.gz)"', resp.text)
                for href in hrefs:
                    name = href.split("/")[-1]
                    url = href if href.startswith("http") else f"{BASE_URL}/file/d/{name}"
                    files.append(RemoteFile(
                        url=url,
                        name=name,
                        file_type=_detect_type(name),
                    ))

            logger.info("Rami Levy: found %d files", len(files))
        except Exception as exc:
            logger.error("Rami Levy: file listing failed: %s", exc)

        return files

    def download_and_decompress(self, url: str) -> bytes:
        if not self._logged_in and not self._login():
            raise RuntimeError("Not logged in to Cerberus")
        return super().download_and_decompress(url)
