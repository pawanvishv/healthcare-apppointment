"""Kafka consumer loop."""

from __future__ import annotations

import logging
import signal

from kafka import KafkaConsumer, TopicPartition, OffsetAndMetadata
from kafka.errors import NoBrokersAvailable

from config import Settings, load_settings
from db import NotificationRepository
from handlers import EventHandler
from notifications import NotificationService

logger = logging.getLogger(__name__)


class GracefulExit:
    def __init__(self) -> None:
        self._stop = False
        signal.signal(signal.SIGINT, self._handle)
        signal.signal(signal.SIGTERM, self._handle)

    def _handle(self, signum, frame) -> None:
        logger.info("Shutdown signal received (%s)", signum)
        self._stop = True

    @property
    def should_stop(self) -> bool:
        return self._stop


def create_consumer(settings: Settings) -> KafkaConsumer:
    return KafkaConsumer(
        *settings.kafka_topics,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        enable_auto_commit=False,
        auto_offset_reset="earliest",
        value_deserializer=lambda value: value,
        consumer_timeout_ms=1000,
    )


def _commit_message(consumer: KafkaConsumer, message) -> None:
    """Commit only the successfully processed message offset."""
    tp = TopicPartition(message.topic, message.partition)
    consumer.commit({tp: OffsetAndMetadata(message.offset + 1, None)})


def run() -> None:
    settings = load_settings()
    logging.basicConfig(
        level=getattr(logging, settings.log_level, logging.INFO),
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )

    repository = NotificationRepository(settings)
    notifier = NotificationService(settings)
    handler = EventHandler(repository, notifier)
    shutdown = GracefulExit()

    logger.info(
        "Starting worker group=%s topics=%s",
        settings.kafka_group_id,
        settings.kafka_topics,
    )

    try:
        consumer = create_consumer(settings)
    except NoBrokersAvailable as exc:
        logger.error("Kafka broker unavailable at %s", settings.kafka_bootstrap_servers)
        raise SystemExit(1) from exc

    try:
        while not shutdown.should_stop:
            polled = consumer.poll(timeout_ms=1000, max_records=10)
            if not polled:
                continue

            for messages in polled.values():
                for message in messages:
                    try:
                        if message.value is None:
                            logger.warning(
                                "Skipping tombstone message topic=%s offset=%s",
                                message.topic,
                                message.offset,
                            )
                            _commit_message(consumer, message)
                            continue

                        handler.handle(message.topic, message.key, message.value)
                        _commit_message(consumer, message)
                        logger.info(
                            "Processed topic=%s partition=%s offset=%s",
                            message.topic,
                            message.partition,
                            message.offset,
                        )
                    except Exception:
                        logger.exception(
                            "Failed to process topic=%s offset=%s",
                            message.topic,
                            message.offset,
                        )
    finally:
        consumer.close()
        logger.info("Worker stopped")


if __name__ == "__main__":
    run()
