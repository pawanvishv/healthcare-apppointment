"""Database access for notification status and patient lookups (H2 via JDBC)."""

from __future__ import annotations

import logging
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Any, Generator, Optional

import jaydebeapi

from config import Settings

logger = logging.getLogger(__name__)


def _row_to_dict(cursor: Any, row: tuple | None) -> Optional[dict]:
    if row is None:
        return None
    columns = [desc[0].lower() for desc in cursor.description]
    return dict(zip(columns, row))


class NotificationRepository:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    @contextmanager
    def _connection(self) -> Generator:
        conn = jaydebeapi.connect(
            "org.h2.Driver",
            self._settings.h2_jdbc_url,
            [self._settings.h2_user, self._settings.h2_password],
            self._settings.h2_jar_path,
        )
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def get_patient_contact(self, patient_id: str) -> Optional[dict]:
        query = """
            SELECT u.email, p.phone
            FROM patients p
            JOIN users u ON p.user_id = u.id
            WHERE p.id = ?
        """
        with self._connection() as conn:
            cursor = conn.cursor()
            try:
                cursor.execute(query, (patient_id,))
                return _row_to_dict(cursor, cursor.fetchone())
            finally:
                cursor.close()

    def mark_attempted(self, appointment_id: str) -> None:
        now = _utc_timestamp()
        query = """
            UPDATE notification_status
            SET attempted_at = ?
            WHERE appointment_id = ?
        """
        self._execute(query, (now, appointment_id))

    def mark_notified(self, appointment_id: str) -> None:
        now = _utc_timestamp()
        query = """
            UPDATE notification_status
            SET status = 'NOTIFIED',
                attempted_at = COALESCE(attempted_at, ?),
                processed_at = ?
            WHERE appointment_id = ?
        """
        self._execute(query, (now, now, appointment_id))

    def mark_failed(self, appointment_id: str) -> None:
        now = _utc_timestamp()
        query = """
            UPDATE notification_status
            SET status = 'FAILED',
                attempted_at = COALESCE(attempted_at, ?)
            WHERE appointment_id = ?
        """
        self._execute(query, (now, appointment_id))

    def notification_row_exists(self, appointment_id: str) -> bool:
        query = "SELECT 1 FROM notification_status WHERE appointment_id = ?"
        with self._connection() as conn:
            cursor = conn.cursor()
            try:
                cursor.execute(query, (appointment_id,))
                return cursor.fetchone() is not None
            finally:
                cursor.close()

    def _execute(self, query: str, params: tuple) -> None:
        with self._connection() as conn:
            cursor = conn.cursor()
            try:
                cursor.execute(query, params)
            finally:
                cursor.close()


def _utc_timestamp() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
