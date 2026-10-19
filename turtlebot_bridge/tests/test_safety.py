"""
Unit tests for SafetyWatchdog engine and fail-safe mechanics.
"""

import asyncio
import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from safety import SafetyWatchdog
from config import STOP_BURST_COUNT


class TestSafety(unittest.IsolatedAsyncioTestCase):

    async def asyncSetUp(self):
        self.published_twists = []
        self.zero_publish_count = 0

        def mock_zero():
            self.zero_publish_count += 1
            self.published_twists.append((0.0, 0.0))

        def mock_twist(x, z):
            self.published_twists.append((x, z))

        self.watchdog = SafetyWatchdog(
            publish_zero_fn=mock_zero,
            publish_twist_fn=mock_twist,
            max_linear=0.15,
            max_angular=1.0,
            lease_timeout=0.15,  # Short timeout for testing (150ms)
        )
        await self.watchdog.start()

    async def asyncTearDown(self):
        await self.watchdog.stop_watchdog()

    async def test_initial_state_is_stopped(self):
        self.assertFalse(self.watchdog.is_moving)
        self.assertTrue(self.watchdog.emergency_stopped)
        self.assertGreaterEqual(self.zero_publish_count, STOP_BURST_COUNT)

    async def test_single_client_ownership_lock(self):
        # Client 1 acquires ownership
        ok, _ = await self.watchdog.process_cmd_vel("client-1", 0.10, 0.0)
        self.assertTrue(ok)
        self.assertEqual(self.watchdog.active_client_id, "client-1")

        # Client 2 tries to send command -> rejected!
        ok2, reason2 = await self.watchdog.process_cmd_vel("client-2", 0.05, 0.5)
        self.assertFalse(ok2)
        self.assertIn("locked", reason2.lower())

    async def test_command_lease_expiration_stops_motors(self):
        # Issue movement command
        ok, _ = await self.watchdog.process_cmd_vel("client-1", 0.10, 0.0)
        self.assertTrue(ok)
        self.assertTrue(self.watchdog.is_moving)

        initial_zeros = self.zero_publish_count

        # Wait longer than lease timeout (150ms + margin)
        await asyncio.sleep(0.25)

        # Watchdog should have fired and stopped the motors
        self.assertFalse(self.watchdog.is_moving)
        self.assertTrue(self.watchdog.emergency_stopped)
        self.assertGreater(self.zero_publish_count, initial_zeros)
        # Verify the last published command was (0.0, 0.0)
        self.assertEqual(self.published_twists[-1], (0.0, 0.0))

    async def test_client_disconnect_triggers_immediate_stop(self):
        # Issue movement command
        await self.watchdog.process_cmd_vel("client-1", 0.12, 0.3)
        self.assertTrue(self.watchdog.is_moving)

        zeros_before = self.zero_publish_count

        # Simulate client disconnection
        self.watchdog.unregister_client("client-1")

        # Motors must be stopped immediately
        self.assertFalse(self.watchdog.is_moving)
        self.assertIsNone(self.watchdog.active_client_id)
        self.assertGreaterEqual(self.zero_publish_count - zeros_before, STOP_BURST_COUNT)
        self.assertEqual(self.published_twists[-1], (0.0, 0.0))

    async def test_explicit_stop_command(self):
        await self.watchdog.process_cmd_vel("client-1", 0.10, 0.0)
        self.assertTrue(self.watchdog.is_moving)

        self.watchdog.trigger_stop(reason="User emergency stop")
        self.assertFalse(self.watchdog.is_moving)
        self.assertTrue(self.watchdog.emergency_stopped)
        self.assertEqual(self.published_twists[-1], (0.0, 0.0))


if __name__ == "__main__":
    unittest.main()
