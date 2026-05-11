# GZ Price Checker — בודק מחירי GZ

Hourly downloader and price-comparison web app for Israeli supermarket GZ price files (mandatory XML publications under Israeli consumer-protection law).

## Supported chains
| Chain | Chain ID |
|---|---|
| שופרסל (Shufersal) | 7290058140886 |
| רמי לוי (Rami Levy) | 7290058108879 |
| ויקטורי (Victory) | 7290696200003 |
| מגה (Mega) | 7290055700007 |

## How it works
1. **Hourly scheduler** (APScheduler) calls `list_files()` on each chain scraper.
2. New `.gz` files (not yet in `sync_logs`) are downloaded and decompressed.
3. XML is parsed into `products` + `prices` tables (SQLite).
4. The React frontend queries the FastAPI backend to search products and compare prices.

## Quick start (Docker)

```bash
cd price-checker
docker compose up --build
```

- Frontend: http://localhost:5173
- API docs: http://localhost:8000/docs

## Quick start (local dev)

### Backend
```bash
cd price-checker/backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### Frontend
```bash
cd price-checker/frontend
npm install
npm run dev
```

## API endpoints
| Method | Path | Description |
|---|---|---|
| GET | `/api/chains` | List all chains + last sync time |
| GET | `/api/products?q=&chain_id=` | Search products (paginated) |
| GET | `/api/products/{item_code}` | Product detail + prices across all chains |
| GET | `/api/sync/status` | Chains + recent sync logs |
| POST | `/api/sync/trigger` | Trigger manual sync immediately |
| GET | `/health` | Health check |

## Adding a new chain
1. Create `backend/app/scrapers/<chain_name>.py` inheriting from `BaseScraper`.
2. Implement `list_files() -> list[RemoteFile]`.
3. Register in `backend/app/scrapers/__init__.py`.
