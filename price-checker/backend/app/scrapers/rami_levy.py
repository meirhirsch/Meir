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

    def _parse_file_entries(self, data) -> list[RemoteFile]:
        if isinstance(data, list):
            entries = data
        elif isinstance(data, dict):
            entries = data.get("data", data.get("files", data.get("Data", data.get("result", []))))
        else:
            entries = []
        files = []
        for entry in entries:
            name = entry if isinstance(entry, str) else (
                entry.get("name") or entry.get("fileName")
                or entry.get("FileNm") or entry.get("FileName") or "")
            if name and name.endswith(".gz"):
                files.append(RemoteFile(url=f"{BASE_URL}/file/d/{name}",
                                        name=name, file_type=_detect_type(name)))
        return files

    def _find_data_url(self, spa_html: str) -> list[str]:
        """Extract AJAX data URLs from the Cerberus JS bundle."""
        import urllib.parse
        candidates = []
        script_srcs = re.findall(r'<script[^>]+src=["\']([^"\']+)["\']', spa_html)
        for src in script_srcs:
            url = src if src.startswith("http") else f"{BASE_URL}{src}"
            try:
                r = self.client.get(url, timeout=20)
                if r.status_code != 200:
                    continue
                js = r.text
                # Kendo DataSource read URL patterns
                for m in re.findall(r'(?:url|read)\s*:\s*["\']([^"\']{3,80})["\']', js):
                    if any(k in m.lower() for k in ["file", "list", "data", "api"]):
                        candidates.append(m if m.startswith("http") else f"{BASE_URL}{m}")
                # fetch() or $.ajax() calls
                for m in re.findall(r'fetch\(["\']([^"\']{3,80})["\']', js):
                    candidates.append(m if m.startswith("http") else f"{BASE_URL}{m}")
                logger.debug("Rami Levy JS %s: found %d candidate URLs", src, len(candidates))
            except Exception as exc:
                logger.debug("Rami Levy JS fetch %s: %s", src, exc)
        return list(dict.fromkeys(candidates))  # deduplicate preserving order

    def list_files(self) -> list[RemoteFile]:
        if not self._logged_in and not self._login():
            return []

        files: list[RemoteFile] = []
        try:
            import time
            ts = int(time.time() * 1000)
            ajax_headers = {
                "Accept": "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": f"{BASE_URL}/file/d",
            }

            # Fetch the SPA shell once
            spa_resp = self.client.get(f"{BASE_URL}/file/d")
            spa_html = spa_resp.text

            # Try 1: Kendo UI datasource POST
            for post_url in [f"{BASE_URL}/file/d", f"{BASE_URL}/file"]:
                try:
                    r = self.client.post(
                        post_url,
                        data={"take": "500", "skip": "0", "page": "1", "pageSize": "500"},
                        headers={**ajax_headers, "Content-Type": "application/x-www-form-urlencoded"},
                    )
                    if r.status_code == 200 and "json" in r.headers.get("content-type", ""):
                        files = self._parse_file_entries(r.json())
                        if files:
                            logger.info("Rami Levy: found %d files via POST %s", len(files), post_url)
                            return files
                except Exception as exc:
                    logger.debug("Rami Levy POST %s: %s", post_url, exc)

            # Try 2: common REST GET patterns
            get_paths = [
                f"/file/d?take=500&skip=0&page=1&pageSize=500&_={ts}",
                f"/file/d.json?_={ts}",
                f"/file/list?_={ts}",
                "/file/d?format=json",
                f"/api/file?_={ts}",
                f"/api/files?_={ts}",
            ]
            for path in get_paths:
                try:
                    r = self.client.get(f"{BASE_URL}{path}", headers=ajax_headers)
                    if r.status_code != 200 or "/login" in str(r.url):
                        continue
                    try:
                        data = r.json()
                        files = self._parse_file_entries(data)
                        if files:
                            logger.info("Rami Levy: found %d files via GET %s", len(files), path)
                            return files
                        logger.debug("Rami Levy GET %s → JSON 0 entries: %s", path, str(data)[:200])
                    except Exception:
                        names = re.findall(r'[\w\.\-]+\.gz', r.text)
                        gz = [n for n in set(names) if any(k in n.lower() for k in ["price", "promo", "store"])]
                        if gz:
                            files = [RemoteFile(url=f"{BASE_URL}/file/d/{n}", name=n,
                                                file_type=_detect_type(n)) for n in gz]
                            logger.info("Rami Levy: found %d .gz in HTML at %s", len(files), path)
                            return files
                except Exception as exc:
                    logger.debug("Rami Levy GET %s: %s", path, exc)

            # Try 3: scan JS bundles for the actual data API URL
            logger.info("Rami Levy: scanning JS bundles for data API endpoint...")
            data_urls = self._find_data_url(spa_html)
            logger.info("Rami Levy: JS scan found %d candidate URLs: %s", len(data_urls), data_urls[:10])

            # Also add common cftp listing patterns based on /file/json/ prefix we discovered
            cftp_listing = [
                f"{BASE_URL}/file/json/ls",
                f"{BASE_URL}/file/json/list",
                f"{BASE_URL}/file/json/dir",
                f"{BASE_URL}/file/json/files",
                f"{BASE_URL}/file/json/browse",
                f"{BASE_URL}/file/json/index",
            ]
            all_candidates = cftp_listing + [u for u in data_urls if u not in cftp_listing]

            for url in all_candidates:
                # Try both GET and POST since some endpoints return 405 on GET
                for method in ("get", "post"):
                    try:
                        if method == "get":
                            r = self.client.get(url, headers=ajax_headers)
                        else:
                            r = self.client.post(
                                url,
                                data={"take": "500", "skip": "0", "page": "1", "pageSize": "500"},
                                headers={**ajax_headers, "Content-Type": "application/x-www-form-urlencoded"},
                            )
                        logger.debug("Rami Levy %s %s → %d ct=%s",
                                     method.upper(), url, r.status_code,
                                     r.headers.get("content-type", "")[:40])
                        if r.status_code == 200:
                            try:
                                data = r.json()
                                files = self._parse_file_entries(data)
                                if files:
                                    logger.info("Rami Levy: found %d files via %s %s",
                                                len(files), method.upper(), url)
                                    return files
                                logger.debug("Rami Levy %s %s JSON 0 entries: %s",
                                             method.upper(), url, str(data)[:200])
                            except Exception:
                                pass
                    except Exception as exc:
                        logger.debug("Rami Levy %s %s: %s", method.upper(), url, exc)

            logger.warning("Rami Levy: all methods returned 0 files")
        except Exception as exc:
            logger.error("Rami Levy: file listing failed: %s", exc)

        logger.info("Rami Levy: total %d files", len(files))
        return files

    def download_and_decompress(self, url: str) -> bytes:
        if not self._logged_in and not self._login():
            raise RuntimeError("Not logged in to Cerberus")
        return super().download_and_decompress(url)
