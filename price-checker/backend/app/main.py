import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.database import init_db
from app.api.routes import router
from app import scheduler

logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
)
# Keep noisy libraries at INFO
logging.getLogger("httpx").setLevel(logging.INFO)
logging.getLogger("httpcore").setLevel(logging.INFO)
logging.getLogger("watchfiles").setLevel(logging.WARNING)
logging.getLogger("apscheduler").setLevel(logging.INFO)

app = FastAPI(title="GZ Price Checker", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/api")


@app.on_event("startup")
def startup():
    init_db()
    scheduler.start()


@app.on_event("shutdown")
def shutdown():
    scheduler.stop()


@app.get("/health")
def health():
    return {"status": "ok"}
