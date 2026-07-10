"""Application configuration loaded from environment variables."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


def resolve_h2_jar() -> str:
    """Locate the H2 JDBC driver JAR (env var, Maven cache, or backend build)."""
    if custom := os.getenv("H2_JAR_PATH"):
        path = Path(custom)
        if path.exists():
            return str(path.resolve())
        raise FileNotFoundError(f"H2_JAR_PATH not found: {custom}")

    m2_root = Path.home() / ".m2" / "repository" / "com" / "h2database" / "h2"
    if m2_root.exists():
        for version_dir in sorted(m2_root.iterdir(), reverse=True):
            jar = version_dir / f"h2-{version_dir.name}.jar"
            if jar.exists():
                return str(jar.resolve())

    backend_dependency = (
        Path(__file__).resolve().parent.parent
        / "backend"
        / "target"
        / "dependency"
    )
    if backend_dependency.exists():
        jars = list(backend_dependency.glob("h2-*.jar"))
        if jars:
            return str(jars[0].resolve())

    raise FileNotFoundError(
        "H2 JDBC driver not found. Run in backend/: "
        "mvn dependency:copy-dependencies -DincludeArtifactIds=h2 "
        "or set H2_JAR_PATH to your h2-*.jar"
    )


@dataclass(frozen=True)
class Settings:
    kafka_bootstrap_servers: str
    kafka_group_id: str
    kafka_topics: list[str]
    h2_jdbc_url: str
    h2_user: str
    h2_password: str
    h2_jar_path: str
    notification_mode: str
    smtp_host: str
    smtp_port: int
    smtp_user: str
    smtp_password: str
    smtp_from: str
    log_level: str


def load_settings() -> Settings:
    topics = os.getenv(
        "KAFKA_TOPICS",
        "appointment.booked,appointment.cancelled,appointment.rescheduled",
    )

    h2_host = os.getenv("H2_HOST", "localhost")
    h2_port = os.getenv("H2_PORT", "9093")
    h2_db_path = os.getenv("H2_DB_PATH", "./data/appointments")
    # TCP URL connects to Spring Boot's H2 AUTO_SERVER instance
    default_jdbc = f"jdbc:h2:tcp://{h2_host}:{h2_port}/{h2_db_path}"

    return Settings(
        kafka_bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        kafka_group_id=os.getenv("KAFKA_GROUP_ID", "healthcare-notification-worker"),
        kafka_topics=[topic.strip() for topic in topics.split(",") if topic.strip()],
        h2_jdbc_url=os.getenv("H2_JDBC_URL", default_jdbc),
        h2_user=os.getenv("H2_USER", "sa"),
        h2_password=os.getenv("H2_PASSWORD", ""),
        h2_jar_path=resolve_h2_jar(),
        notification_mode=os.getenv("NOTIFICATION_MODE", "mock").lower(),
        smtp_host=os.getenv("SMTP_HOST", ""),
        smtp_port=int(os.getenv("SMTP_PORT", "587")),
        smtp_user=os.getenv("SMTP_USER", ""),
        smtp_password=os.getenv("SMTP_PASSWORD", ""),
        smtp_from=os.getenv("SMTP_FROM", "noreply@healthapp.com"),
        log_level=os.getenv("LOG_LEVEL", "INFO").upper(),
    )
