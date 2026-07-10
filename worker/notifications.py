"""Notification delivery (mock logger or SMTP)."""

from __future__ import annotations

import logging
import smtplib
from email.message import EmailMessage

from config import Settings

logger = logging.getLogger(__name__)


class NotificationService:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def send(self, to_email: str, subject: str, body: str) -> None:
        if self._settings.notification_mode == "smtp" and self._settings.smtp_host:
            self._send_smtp(to_email, subject, body)
        else:
            logger.info(
                "MOCK EMAIL -> to=%s subject=%s body=%s",
                to_email,
                subject,
                body.replace("\n", " | "),
            )

    def _send_smtp(self, to_email: str, subject: str, body: str) -> None:
        message = EmailMessage()
        message["From"] = self._settings.smtp_from
        message["To"] = to_email
        message["Subject"] = subject
        message.set_content(body)

        with smtplib.SMTP(self._settings.smtp_host, self._settings.smtp_port) as server:
            server.starttls()
            if self._settings.smtp_user:
                server.login(self._settings.smtp_user, self._settings.smtp_password)
            server.send_message(message)

        logger.info("SMTP email sent to %s", to_email)
