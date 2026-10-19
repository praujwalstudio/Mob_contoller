"""
TurtleBot 3 Mock Bridge Server
Runs the WebSocket bridge in standalone simulation mode with interactive console logging.
Useful for testing the Android App on a PC without ROS or physical robot hardware.
Compatible with Python 3.8 through 3.14.
"""

import asyncio
import logging
from ros_adapter import MockROSAdapter
from server import RobotBridgeServer
import config

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("MockBridge")


async def _async_run():
    mock_adapter = MockROSAdapter()
    server = RobotBridgeServer(
        host="0.0.0.0",
        port=config.DEFAULT_PORT,
        adapter=mock_adapter,
        max_linear=config.DEFAULT_MAX_LINEAR_VELOCITY,
        max_angular=config.DEFAULT_MAX_ANGULAR_VELOCITY,
    )
    await server.start()
    try:
        while True:
            await asyncio.sleep(1)
    except asyncio.CancelledError:
        pass
    finally:
        await server.stop()


def run_mock():
    print("=" * 65)
    print("  TURTLEBOT 3 MOCK ROBOT BRIDGE (DESKTOP SIMULATION)")
    print(f"  Listening on: ws://0.0.0.0:{config.DEFAULT_PORT}")
    print(f"  Command Lease: {config.COMMAND_LEASE_TIMEOUT_SEC * 1000:.0f} ms")
    print(f"  Linear Speed Limit: {config.DEFAULT_MAX_LINEAR_VELOCITY} m/s")
    print(f"  Angular Speed Limit: {config.DEFAULT_MAX_ANGULAR_VELOCITY} rad/s")
    print("=" * 65)

    try:
        asyncio.run(_async_run())
    except KeyboardInterrupt:
        print("\nStopping mock bridge...")


if __name__ == "__main__":
    run_mock()
