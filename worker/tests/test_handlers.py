import json
from unittest.mock import MagicMock

import pytest

from handlers import (
    EventHandler,
    TOPIC_BOOKED,
    TOPIC_CANCELLED,
    TOPIC_RESCHEDULED,
    parse_event_payload,
)


@pytest.fixture
def handler() -> EventHandler:
    repository = MagicMock()
    repository.notification_row_exists.return_value = True
    repository.get_patient_contact.return_value = {
        "email": "patient@healthapp.com",
        "phone": "+1234567890",
    }
    notifier = MagicMock()
    return EventHandler(repository, notifier)


def test_handle_booked_updates_status_and_sends_email(handler: EventHandler) -> None:
    payload = {
        "schemaVersion": 1,
        "appointmentId": "apt-1",
        "patientId": "p-201",
        "doctorId": "d-101",
        "slotId": "s-001",
        "startTime": "2026-07-10T09:00:00Z",
        "status": "CONFIRMED",
    }

    handler.handle(TOPIC_BOOKED, "apt-1", json.dumps(payload).encode("utf-8"))

    handler._repository.mark_attempted.assert_called_once_with("apt-1")
    handler._repository.mark_notified.assert_called_once_with("apt-1")
    handler._notifier.send.assert_called_once()
    args = handler._notifier.send.call_args[0]
    assert args[0] == "patient@healthapp.com"
    assert "Appointment confirmed" in args[1]


def test_handle_cancelled_sends_without_status_update(handler: EventHandler) -> None:
    payload = {
        "schemaVersion": 1,
        "appointmentId": "apt-2",
        "patientId": "p-201",
        "doctorId": "d-101",
        "status": "CANCELLED",
    }

    handler.handle(TOPIC_CANCELLED, "apt-2", json.dumps(payload).encode("utf-8"))

    handler._repository.mark_attempted.assert_not_called()
    handler._repository.mark_notified.assert_not_called()
    handler._notifier.send.assert_called_once()


def test_handle_rescheduled_marks_failed_on_error(handler: EventHandler) -> None:
    handler._repository.get_patient_contact.side_effect = ValueError("missing patient")
    payload = {
        "schemaVersion": 1,
        "appointmentId": "apt-3",
        "patientId": "p-999",
        "doctorId": "d-101",
        "oldSlotId": "s-001",
        "newSlotId": "s-002",
        "status": "CONFIRMED",
    }

    with pytest.raises(ValueError):
        handler.handle(
            TOPIC_RESCHEDULED,
            "apt-3",
            json.dumps(payload).encode("utf-8"),
        )

    handler._repository.mark_failed.assert_called_once_with("apt-3")


def test_parse_event_payload_handles_double_encoded_json() -> None:
    inner = {"appointmentId": "apt-1", "patientId": "p-201"}
    raw = json.dumps(json.dumps(inner)).encode("utf-8")
    assert parse_event_payload(raw) == inner


def test_parse_event_payload_rejects_empty() -> None:
    with pytest.raises(ValueError, match="Empty event payload"):
        parse_event_payload(None)


def test_handle_booked_skips_db_when_row_missing(handler: EventHandler) -> None:
    handler._repository.notification_row_exists.return_value = False
    payload = {
        "appointmentId": "apt-4",
        "patientId": "p-201",
        "doctorId": "d-101",
        "slotId": "s-001",
        "status": "CONFIRMED",
    }

    handler.handle(TOPIC_BOOKED, "apt-4", json.dumps(payload).encode("utf-8"))

    handler._repository.mark_attempted.assert_not_called()
    handler._repository.mark_notified.assert_not_called()
    handler._notifier.send.assert_called_once()
