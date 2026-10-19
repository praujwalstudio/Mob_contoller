"""
TurtleBot 3 Protocol Specification (Version 1)
Handles serialization, deserialization, and strict validation of WebSocket messages.
"""

import json
import time
from typing import Any, Dict, Optional, Tuple

PROTOCOL_VERSION = 1

# Supported message types
TYPE_CMD_VEL = "cmd_vel"
TYPE_STOP = "stop"
TYPE_PING = "ping"
TYPE_PONG = "pong"
TYPE_TELEMETRY = "telemetry"
TYPE_ACK = "ack"
TYPE_ERROR = "error"


class ProtocolError(Exception):
    """Raised when a protocol message fails validation."""
    pass


def parse_message(raw_text: str) -> Dict[str, Any]:
    """
    Parses a raw JSON string and validates basic envelope requirements.
    Raises ProtocolError if malformed or invalid version.
    """
    try:
        data = json.loads(raw_text)
    except (json.JSONDecodeError, TypeError) as exc:
        raise ProtocolError(f"Malformed JSON: {exc}")

    if not isinstance(data, dict):
        raise ProtocolError("Message root must be a JSON object")

    version = data.get("version")
    if version != PROTOCOL_VERSION:
        raise ProtocolError(f"Unsupported protocol version: {version}. Expected: {PROTOCOL_VERSION}")

    msg_type = data.get("type")
    if not isinstance(msg_type, str) or not msg_type.strip():
        raise ProtocolError("Missing or invalid 'type' field in message envelope")

    return data


def validate_and_clamp_cmd_vel(
    data: Dict[str, Any],
    max_linear: float,
    max_angular: float
) -> Tuple[float, float, str]:
    """
    Validates a 'cmd_vel' payload, ensures linear_x and angular_z are finite floats,
    clamps them within safety limits, and returns (clamped_linear, clamped_angular, command_id).
    """
    linear_x = data.get("linear_x")
    angular_z = data.get("angular_z")
    command_id = str(data.get("command_id", ""))

    if not isinstance(linear_x, (int, float)) or isinstance(linear_x, bool):
        raise ProtocolError("Field 'linear_x' must be a numeric value")

    if not isinstance(angular_z, (int, float)) or isinstance(angular_z, bool):
        raise ProtocolError("Field 'angular_z' must be a numeric value")

    linear_x = float(linear_x)
    angular_z = float(angular_z)

    # Check for NaN / Infinity
    if not (-1e6 < linear_x < 1e6) or not (-1e6 < angular_z < 1e6):
        raise ProtocolError("Velocity values must be finite numbers")

    # Strict clamping to safety boundaries
    clamped_linear = max(-max_linear, min(max_linear, linear_x))
    clamped_angular = max(-max_angular, min(max_angular, angular_z))

    return clamped_linear, clamped_angular, command_id


def create_ack(command_id: str, status: str = "ok", reason: str = "") -> str:
    """Builds an acknowledgement message."""
    payload = {
        "version": PROTOCOL_VERSION,
        "type": TYPE_ACK,
        "command_id": command_id,
        "status": status,
        "reason": reason,
        "timestamp": time.time()
    }
    return json.dumps(payload)


def create_pong(client_timestamp: Optional[float] = None) -> str:
    """Builds a pong response to client ping."""
    payload = {
        "version": PROTOCOL_VERSION,
        "type": TYPE_PONG,
        "client_timestamp": client_timestamp,
        "server_timestamp": time.time()
    }
    return json.dumps(payload)


def create_error(message: str, command_id: Optional[str] = None) -> str:
    """Builds a standardized error message."""
    payload = {
        "version": PROTOCOL_VERSION,
        "type": TYPE_ERROR,
        "message": message,
        "command_id": command_id,
        "timestamp": time.time()
    }
    return json.dumps(payload)


def create_telemetry(
    linear_velocity: float,
    angular_velocity: float,
    battery_voltage: float = 12.0,
    is_stopped: bool = True,
    has_active_lease: bool = False,
    active_client: Optional[str] = None
) -> str:
    """Builds live telemetry stream message."""
    payload = {
        "version": PROTOCOL_VERSION,
        "type": TYPE_TELEMETRY,
        "linear_velocity": round(linear_velocity, 4),
        "angular_velocity": round(angular_velocity, 4),
        "battery_voltage": round(battery_voltage, 2),
        "is_stopped": is_stopped,
        "has_active_lease": has_active_lease,
        "active_client": active_client,
        "timestamp": time.time()
    }
    return json.dumps(payload)
