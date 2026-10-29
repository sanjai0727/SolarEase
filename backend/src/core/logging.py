import logging
import sys

def setup_logging():
    logging.basicConfig(
        stream=sys.stdout,
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    # Configure root logger
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    
    # SQLAlchemy logging configuration can go here
    logging.getLogger("sqlalchemy.engine").setLevel(logging.WARNING)

    return logger

logger = setup_logging()
