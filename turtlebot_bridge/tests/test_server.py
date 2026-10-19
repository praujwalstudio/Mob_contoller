"""
Integration tests for RobotBridgeServer over WebSocket.
"""

import asyncio
import json
import os
import sys
import unittest
import websockets

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from ros_adapter import MockROSAdapter
from server import RobotBridgeServer
from protocol import TYPE_ACK, TYPE_CMD_VEL, TYPE_PING, TYPE_PONG, TYPE_STOP, TYPE_TELEMETRY


class TestServerIntegration(unittest.IsolatedAsyncioTestCase):

    async def asyncSetUp(self):
        self.mock_adapter = MockROSAdapter()
        # Bind to localhost on dynamic free port or standard test port
        self.port = 18765
        self.server = RobotBridgeServer(
            host="127.0.0.1",
            port=self.port,
            adapter=self.mock_adapter,
            max_linear=0.20,
            max_angular=1.5,
        )
        await self.server.start()

    async def asyncTearDown(self):
        await self.server.stop()

    async def test_connect_and_receive_initial_telemetry(self):
        uri = f"ws://127.0.0.1:{self.port}"
        async with websockets.connect(uri) as ws:
            # First message from server should be initial telemetry
            first_msg_raw = await asyncio.wait_for(ws.recv(), timeout=2.0)
            first_msg = json.loads(first_msg_raw)
            self.assertEqual(first_msg["type"], TYPE_TELEMETRY)
            self.assertTrue(first_msg["is_stopped"])

    async def test_ping_pong(self):
        uri = f"ws://127.0.0.1:{self.port}"
        async with websockets.connect(uri) as ws:
            # Consume initial telemetry
            await ws.recv()

            ping_payload = json.dumps({"version": 1, "type": TYPE_PING, "timestamp": 1234.5})
            await ws.send(ping_payload)

            pong_raw = await asyncio.wait_for(ws.recv(), timeout=2.0)
            pong = json.loads(pong_raw)
            self.assertEqual(pong["type"], TYPE_PONG)
            self.assertEqual(pong["client_timestamp"], 1234.5)

    async def test_cmd_vel_and_stop_flow(self):
        uri = f"ws://127.0.0.1:{self.port}"
        async with websockets.connect(uri) as ws:
            # Consume initial telemetry
            await ws.recv()

            # Send movement command
            cmd = {
                "version": 1,
                "type": TYPE_CMD_VEL,
                "linear_x": 0.15,
                "angular_z": 0.0,
                "command_id": "test-mv-1"
            }
            await ws.send(json.dumps(cmd))

            ack_raw = await asyncio.wait_for(ws.recv(), timeout=2.0)
            ack = json.loads(ack_raw)
            self.assertEqual(ack["type"], TYPE_ACK)
            self.assertEqual(ack["command_id"], "test-mv-1")
            self.assertEqual(ack["status"], "ok")

            # Check that mock adapter received the command
            self.assertAlmostEqual(self.mock_adapter.latest_linear_x, 0.15)

            # Send explicit STOP
            stop_cmd = {
                "version": 1,
                "type": TYPE_STOP,
                "command_id": "test-stop-1"
            }
            await ws.send(json.dumps(stop_cmd))

            stop_ack_raw = await asyncio.wait_for(ws.recv(), timeout=2.0)
            stop_ack = json.loads(stop_ack_raw)
            self.assertEqual(stop_ack["type"], TYPE_ACK)
            self.assertEqual(stop_ack["command_id"], "test-stop-1")

            # Verify velocities are zero
            self.assertEqual(self.mock_adapter.latest_linear_x, 0.0)
            self.assertEqual(self.mock_adapter.latest_angular_z, 0.0)


if __name__ == "__main__":
    unittest.main()
