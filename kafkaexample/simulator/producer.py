import argparse
import json
import random
import signal
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from threading import Event, Lock

from confluent_kafka import Producer


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="IoT Kafka load simulator")
    parser.add_argument("--bootstrap-servers", default="localhost:39092")
    parser.add_argument("--topic", default="sensor_data")
    parser.add_argument("--devices", type=int, default=5000)
    parser.add_argument("--rate", type=int, default=1000, help="Target messages per second")
    parser.add_argument("--duration", type=int, default=0, help="Seconds; 0 means run forever")
    parser.add_argument("--log-interval", type=int, default=5)
    parser.add_argument("--invalid-ratio", type=float, default=0.0)
    parser.add_argument("--unknown-device-ratio", type=float, default=0.0)
    return parser.parse_args()


@dataclass
class WindowSnapshot:
    submitted: int
    delivered: int
    failed: int
    elapsed_seconds: float


class Stats:
    def __init__(self) -> None:
        self._lock = Lock()
        self._window_started = time.monotonic()
        self._submitted = 0
        self._delivered = 0
        self._failed = 0

    def mark_submitted(self) -> None:
        with self._lock:
            self._submitted += 1

    def delivery_callback(self, error, _message) -> None:
        with self._lock:
            if error is None:
                self._delivered += 1
            else:
                self._failed += 1

    def snapshot_and_reset(self) -> WindowSnapshot:
        with self._lock:
            now = time.monotonic()
            snapshot = WindowSnapshot(
                submitted=self._submitted,
                delivered=self._delivered,
                failed=self._failed,
                elapsed_seconds=max(now - self._window_started, 1e-6),
            )
            self._window_started = now
            self._submitted = 0
            self._delivered = 0
            self._failed = 0
            return snapshot


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def build_payload(
    device_id: str,
    invalid_ratio: float,
    is_unknown_device: bool,
) -> str:
    if invalid_ratio > 0 and random.random() < invalid_ratio:
        variant = random.choice(
            [
                {},
                {"deviceId": device_id, "timestamp": utc_now()},
                {"deviceId": device_id, "value": "", "timestamp": utc_now()},
                {"value": round(random.uniform(15.0, 40.0), 3), "timestamp": utc_now()},
            ]
        )
        return json.dumps(variant, separators=(",", ":"))

    effective_device_id = device_id if not is_unknown_device else f"unknown-{device_id}"
    payload = {
        "deviceId": effective_device_id,
        "value": round(random.uniform(15.0, 40.0), 3),
        "timestamp": utc_now(),
    }
    return json.dumps(payload, separators=(",", ":"))


def build_producer(bootstrap_servers: str) -> Producer:
    return Producer(
        {
            "bootstrap.servers": bootstrap_servers,
            "client.id": "iot-load-simulator",
            "acks": "all",
            "enable.idempotence": True,
            "compression.type": "lz4",
            "linger.ms": 15,
            "batch.size": 131072,
            "queue.buffering.max.messages": 200000,
            "max.in.flight.requests.per.connection": 5,
            "delivery.timeout.ms": 120000,
        }
    )


def main() -> int:
    args = parse_args()
    stop_event = Event()
    signal.signal(signal.SIGINT, lambda *_: stop_event.set())
    signal.signal(signal.SIGTERM, lambda *_: stop_event.set())

    producer = build_producer(args.bootstrap_servers)
    stats = Stats()
    devices = [f"device-{index:04d}" for index in range(1, args.devices + 1)]
    device_cursor = 0
    started_at = time.monotonic()
    last_iteration = started_at
    message_budget = 0.0
    next_log_at = started_at + args.log_interval

    try:
        while not stop_event.is_set():
            now = time.monotonic()
            if args.duration > 0 and now - started_at >= args.duration:
                break

            elapsed = now - last_iteration
            last_iteration = now
            message_budget += args.rate * elapsed
            to_send = int(message_budget)
            message_budget -= to_send

            for _ in range(to_send):
                device_id = devices[device_cursor]
                device_cursor = (device_cursor + 1) % len(devices)

                is_unknown_device = (
                    args.unknown_device_ratio > 0
                    and random.random() < args.unknown_device_ratio
                )
                payload = build_payload(
                    device_id=device_id,
                    invalid_ratio=args.invalid_ratio,
                    is_unknown_device=is_unknown_device,
                )

                submitted = False
                while not submitted and not stop_event.is_set():
                    try:
                        producer.produce(
                            topic=args.topic,
                            key=device_id,
                            value=payload,
                            on_delivery=stats.delivery_callback,
                        )
                        stats.mark_submitted()
                        submitted = True
                    except BufferError:
                        producer.poll(0.1)

                producer.poll(0)

            if now >= next_log_at:
                producer.poll(0)
                snapshot = stats.snapshot_and_reset()
                print(
                    (
                        "[simulator] submitted=%d delivered=%d failed=%d "
                        "submit_rate=%.2f msg/s delivered_rate=%.2f msg/s"
                    )
                    % (
                        snapshot.submitted,
                        snapshot.delivered,
                        snapshot.failed,
                        snapshot.submitted / snapshot.elapsed_seconds,
                        snapshot.delivered / snapshot.elapsed_seconds,
                    ),
                    flush=True,
                )
                next_log_at = now + args.log_interval

            sleep_time = max(0.0, 0.01 - (time.monotonic() - now))
            if sleep_time > 0:
                time.sleep(sleep_time)
    finally:
        producer.flush(10)
        snapshot = stats.snapshot_and_reset()
        print(
            (
                "[simulator] final submitted=%d delivered=%d failed=%d "
                "submit_rate=%.2f msg/s delivered_rate=%.2f msg/s"
            )
            % (
                snapshot.submitted,
                snapshot.delivered,
                snapshot.failed,
                snapshot.submitted / snapshot.elapsed_seconds,
                snapshot.delivered / snapshot.elapsed_seconds,
            ),
            flush=True,
        )

    return 0


if __name__ == "__main__":
    sys.exit(main())
