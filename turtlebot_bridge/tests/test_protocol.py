"""
Unit tests for TurtleBot 3 message protocol and serialization.
"""

import json
import unittest
import sys
import os

# Ensure parent directory is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from protocol import (
    PROTOCOL_VERSION,
    TYPE_ACK,
    TYPE_CMD_VEL,
    TYPE_ERROR,
    TYPE_PING,
    TYPE_PONG,
    TYPE_STOP,
    TYPE_TELEMETRY,
    ProtocolError,
    create_ack,
    create_error,
    create_pong,
    create_telemetry,
    parse_message,
    validate_and_clamp_cmd_vel,
)


class TestProtocol(unittest.TestCase):

    def test_parse_valid_message(self):
        raw = json.dumps({"version": 1, "type": "cmd_vel", "linear_x": 0.1, "angular_z": 0.0})
        parsed = parse_message(raw)
        self.assertEqual(parsed["version"], 1)
        self.assertEqual(parsed["type"], "cmd_vel")

    def test_parse_invalid_json(self):
        with self.assertRaises(ProtocolError):
            parse_message("{invalid_json: 123")

    def test_parse_unsupported_version(self):
        raw = json.dumps({"version": 99, "type": "cmd_vel"})
        with self.assertRaises(ProtocolError) as ctx:
            parse_message(raw)
        self.assertIn("Unsupported protocol version", str(ctx.exception))

    def test_parse_missing_type(self):
        raw = json.dumps({"version": 1})
        with self.assertRaises(ProtocolError) as ctx:
            parse_message(raw)
        self.assertIn("Missing or invalid 'type'", str(ctx.exception))

    def test_validate_cmd_vel_clamping(self):
        data = {
            "version": 1,
            "type": "cmd_vel",
            "linear_x": 1.5,     # Exceeds max
            "angular_z": -5.0,   # Exceeds max
            "command_id": "test-123"
        }
        lin, ang, cmd_id = validate_and_clamp_cmd_vel(data, max_linear=0.22, max_angular=2.84)
        self.assertAlmostEqual(lin, 0.22)
        self.assertAlmostEqual(ang, -2.84)
        self.assertEqual(cmd_id, "test-123")

    def test_validate_cmd_vel_negative_clamping(self):
        data = {
            "version": 1,
            "type": "cmd_vel",
            "linear_x": -0.80,
            "angular_z": 3.0,
            "command_id": "test-neg"
        }
        lin, ang, cmd_id = validate_and_clamp_cmd_vel(data, max_linear=0.15, max_angular=1.5)
        self.assertAlmostEqual(lin, -0.15)
        self.assertAlmostEqual(ang, 1.5)

    def test_validate_cmd_vel_invalid_types(self):
        with self.assertRaises(ProtocolError):
            validate_and_clamp_cmd_vel({"linear_x": "fast", "angular_z": 0.0}, 0.22, 2.84)
        with self.assertRaises(ProtocolError):
            validate_and_clamp_cmd_vel({"linear_x": True, "angular_z": 0.0}, 0.22, 2.84)
        with self.assertRaises(ProtocolError):
            validate_and_clamp_cmd_vel({"linear_x": 0.1, "angular_z": None}, 0.22, 2.84)

    def test_create_ack(self):
        ack_str = create_ack("cmd-1", status="ok", reason="Accepted")
        ack = json.loads(ack_str)
        self.assertEqual(ack["type"], TYPE_ACK)
        self.assertEqual(ack["command_id"], "cmd-1")
        self.assertEqual(ack["status"], "ok")

    def test_create_pong(self):
        pong_str = create_pong(client_timestamp=12345.67)
        pong = json.loads(pong_str)
        self.assertEqual(pong["type"], TYPE_PONG)
        self.assertEqual(pong["client_timestamp"], 12345.67)

    def test_create_telemetry(self):
        tel_str = create_telemetry(linear_velocity=0.12, angular_velocity=-0.45, battery_voltage=12.2)
        tel = json.loads(tel_str)
        self.assertEqual(tel["type"], TYPE_TELEMETRY)
        self.assertEqual(tel["linear_velocity"], 0.12)
        self.assertEqual(tel["angular_velocity"], -0.45)
        self.assertEqual(tel["battery_voltage"], 12.2)


if __name__ == "__main__":
    unittest.main()
