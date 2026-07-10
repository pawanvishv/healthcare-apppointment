"""Kafka event handlers for appointment lifecycle notifications."""

from __future__ import annotations

import json
import logging
from typing import Any

from db import NotificationRepository
from notifications import NotificationService

logger = logging.getLogger(__name__)

TOPIC_BOOKED = "appointment.booked"
TOPIC_CANCELLED = "appointment.cancelled"
TOPIC_RESCHEDULED = "appointment.rescheduled"


def parse_event_payload(raw_value: bytes | None) -> dict[str, Any]:
    """Parse Kafka JSON payload, tolerating double-encoded strings from JsonSerializer."""
    if not raw_value:
        raise ValueError("Empty event payload")

    data: Any = json.loads(raw_value.decode("utf-8"))
    if isinstance(data, str):
        data = json.loads(data)

    if not isinstance(data, dict):
        raise ValueError(f"Expected JSON object, got {type(data).__name__}")

    return data


class EventHandler:
    def __init__(
        self,
        repository: NotificationRepository,
        notifier: NotificationService,
    ) -> None:
        self._repository = repository
        self._notifier = notifier

    def handle(self, topic: str, key: str | bytes | None, raw_value: bytes) -> None:
        payload = parse_event_payload(raw_value)
        decoded_key = key.decode("utf-8") if isinstance(key, bytes) else key
        appointment_id = payload.get("appointmentId") or decoded_key

        if not appointment_id:
            raise ValueError("Event missing appointmentId")

        logger.info("Processing topic=%s appointmentId=%s", topic, appointment_id)

        if topic == TOPIC_BOOKED:
            self._handle_booked(payload)
        elif topic == TOPIC_RESCHEDULED:
            self._handle_rescheduled(payload)
        elif topic == TOPIC_CANCELLED:
            self._handle_cancelled(payload)
        else:
            logger.warning("Unhandled topic: %s", topic)

    def _require_patient_id(self, payload: dict[str, Any]) -> str:
        patient_id = payload.get("patientId")
        if not patient_id:
            raise ValueError("Event missing patientId")
        return patient_id

    def _handle_booked(self, payload: dict[str, Any]) -> None:
        self._notify_with_status(payload, "Appointment confirmed")

    def _handle_rescheduled(self, payload: dict[str, Any]) -> None:
        self._notify_with_status(payload, "Appointment rescheduled")

    def _handle_cancelled(self, payload: dict[str, Any]) -> None:
        patient_id = self._require_patient_id(payload)
        contact = self._repository.get_patient_contact(patient_id)
        if not contact:
            raise ValueError(f"Patient not found: {patient_id}")

        body = self._build_body(
            payload,
            "Your appointment has been cancelled.",
            extra={"status": payload.get("status")},
        )
        self._notifier.send(contact["email"], "Appointment cancelled", body)

    def _notify_with_status(self, payload: dict[str, Any], headline: str) -> None:
        appointment_id = payload["appointmentId"]
        patient_id = self._require_patient_id(payload)
        row_exists = self._repository.notification_row_exists(appointment_id)

        if not row_exists:
            logger.warning(
                "No notification_status row for appointment %s; skipping DB update",
                appointment_id,
            )
        else:
            self._repository.mark_attempted(appointment_id)

        try:
            contact = self._repository.get_patient_contact(patient_id)
            if not contact:
                raise ValueError(f"Patient not found: {patient_id}")

            body = self._build_body(payload, headline)
            self._notifier.send(contact["email"], headline, body)

            if row_exists:
                self._repository.mark_notified(appointment_id)
        except Exception:
            if row_exists:
                self._repository.mark_failed(appointment_id)
            raise

    def _build_body(self, payload: dict[str, Any], headline: str, extra: dict | None = None) -> str:
        lines = [
            headline,
            "",
            f"Appointment ID: {payload.get('appointmentId')}",
            f"Doctor ID: {payload.get('doctorId')}",
            f"Patient ID: {payload.get('patientId')}",
        ]

        if payload.get("slotId"):
            lines.append(f"Slot ID: {payload['slotId']}")
        if payload.get("newSlotId"):
            lines.append(f"New slot ID: {payload['newSlotId']}")
        if payload.get("oldSlotId"):
            lines.append(f"Previous slot ID: {payload['oldSlotId']}")
        if payload.get("startTime"):
            lines.append(f"Start time: {payload['startTime']}")
        if payload.get("status"):
            lines.append(f"Status: {payload['status']}")

        if extra:
            for key, value in extra.items():
                lines.append(f"{key}: {value}")

        lines.append("")
        lines.append("— HealthApp")
        return "\n".join(lines)
