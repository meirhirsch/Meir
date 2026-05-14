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

            # Step 3: Verify session works
            check = self.client.get(f"{BASE_URL}/file/d")
            if "/login" not in str(check.url):
                self._logged_in = True
                logger.info("Rami Levy: logged in successfully")
                return True

            logger.error("Rami Levy: login failed — still redirected to login page")
            return False
        except Exception as exc:
            logger.error("Rami Levy: login failed: %s", exc)
            return False

    def list_files(self) -> list[RemoteFile]:
        if not self._logged_in and not self._login():
            return []

        files: list[RemoteFile] = []
        try:
            # Cerberus is a JS SPA — request the file list as JSON via API
            for endpoint in ["/file/json", "/file/d/json", "/api/file/d"]:
                resp = self.client.get(
                    f"{BASE_URL}{endpoint}",
                    headers={"Accept": "application/json, */*", "X-Requested-With": "XMLHttpRequest"},
                )
                if resp.status_code == 200 and "/login" not in str(resp.url):
                    try:
                        data = resp.json()
                        logger.debug("Rami Levy %s JSON: %s", endpoint, str(data)[:300])
                        entries = data if isinstance(data, list) else data.get("files", data.get("data", []))
                        for entry in entries:
                            name = entry.get("name", "") or entry.get("fileName", "") or entry.get("file_name", "")
                            if name and name.endswith(".gz"):
                                files.append(RemoteFile(
                                    url=f"{BASE_URL}/file/d/{name}",
                                    name=name,
                                    file_type=_detect_type(name),
                                ))
                        if files:
                            logger.info("Rami Levy: found %d files via %s", len(files), endpoint)
                            break
                    except Exception as e:
                        logger.debug("Rami Levy %s not JSON: %s | preview: %s", endpoint, e, resp.text[:200])

            # Fallback: parse HTML for .gz hrefs
            if not files:
                resp = self.client.get(f"{BASE_URL}/file/d")
                hrefs = re.findall(r'href=["\']([^"\']+\.gz)["\']', resp.text)
                for href in hrefs:
                    name = href.split("/")[-1]
                    url = href if href.startswith("http") else f"{BASE_URL}/file/d/{name}"
                    files.append(RemoteFile(url=url, name=name, file_type=_detect_type(name)))
                logger.info("Rami Levy: found %d files via HTML fallback", len(files))
        except Exception as exc:
            logger.error("Rami Levy: file listing failed: %s", exc)

        return files

    def download_and_decompress(self, url: str) -> bytes:
        if not self._logged_in and not self._login():
            raise RuntimeError("Not logged in to Cerberus")
        return super().download_and_decompress(url)
