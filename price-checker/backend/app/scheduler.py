import logging

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger

from app.sync import sync_all

logger = logging.getLogger(__name__)

_scheduler = BackgroundScheduler()


def _run_sync():
    logger.info("Hourly sync starting...")
    results = sync_all()
    for r in results:
        logger.info("Sync result: %s", r)
    logger.info("Hourly sync complete.")


def start():
    _scheduler.add_job(_run_sync, IntervalTrigger(hours=1), id="hourly_sync", replace_existing=True)
    _scheduler.start()
    logger.info("Scheduler started — hourly GZ sync enabled.")


def stop():
    _scheduler.shutdown(wait=False)


def trigger_now():
    """Run a sync immediately (used by the manual-trigger API endpoint)."""
    _run_sync()
