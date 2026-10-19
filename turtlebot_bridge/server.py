"""
TurtleBot 3 WebSocket Bridge Server
Manages incoming client connections, validates movement commands,
dispatches to ROS, and enforces fail-safe hardware protection.
"""

import argparse
import asyncio
import logging
import signal
import sys
import time
from typing import Set

import config
from protocol import (
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
from ros_adapter import BaseROSAdapter, detect_and_create_adapter
from safety import SafetyWatchdog

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("TurtleBotServer")


class RobotBridgeServer:
    """Core WebSocket server bridging Android client with TurtleBot 3 ROS middleware."""

    def __init__(
        self,
        host: str = config.DEFAULT_HOST,
        port: int = config.DEFAULT_PORT,
        adapter: BaseROSAdapter | None = None,
        max_linear: float = config.DEFAULT_MAX_LINEAR_VELOCITY,
        max_angular: float = config.DEFAULT_MAX_ANGULAR_VELOCITY,
    ):
        self.host = host
        self.port = port
        self.adapter = adapter or detect_and_create_adapter()
        self.connected_clients: Set[Any] = set()
        self._running = False
        self._server = None

        # Safety watchdog instance
        self.safety = SafetyWatchdog(
            publish_zero_fn=self.adapter.publish_zero,
            publish_twist_fn=self.adapter.publish_cmd_vel,
            max_linear=max_linear,
            max_angular=max_angular,
            lease_timeout=config.COMMAND_LEASE_TIMEOUT_SEC,
        )

    async def start(self) -> None:
        """Starts the safety engine and WebSocket server."""
        try:
            import websockets
        except ImportError:
            logger.error(
                "Required package 'websockets' not installed. Please run: pip install websockets"
            )
            sys.exit(1)

        self._running = True
        await self.safety.start()

        # Telemetry broadcaster task
        asyncio.create_task(self._telemetry_broadcaster())

        logger.info("Starting TurtleBot 3 WebSocket Bridge on ws://%s:%d ...", self.host, self.port)
        self._server = await websockets.serve(self._handle_client, self.host, self.port)
        logger.info("Bridge is live and awaiting connections.")

    async def stop(self) -> None:
        """Gracefully halts server, motors, and ROS node."""
        logger.info("Shutting down TurtleBot Bridge...")
        self._running = False
        await self.safety.stop_watchdog()
        self.adapter.shutdown()

        if self._server:
            self._server.close()
            await self._server.wait_closed()
        logger.info("Bridge server terminated safely.")

    async def _handle_client(self, websocket) -> None:
        """Handles an individual client WebSocket session."""
        client_address = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
        logger.info("Client connected: %s", client_address)
        self.connected_clients.add(websocket)

        # Attempt to register this client as the active motion controller
        self.safety.register_client(client_address)

        try:
            # Immediately send initial telemetry snapshot upon connection
            telemetry_data = self.adapter.get_telemetry()
            await websocket.send(
                create_telemetry(
                    linear_velocity=telemetry_data["linear_velocity"],
                    angular_velocity=telemetry_data["angular_velocity"],
                    battery_voltage=telemetry_data["battery_voltage"],
                    is_stopped=not self.safety.is_moving,
                    has_active_lease=self.safety.has_active_lease(),
                    active_client=self.safety.active_client_id,
                )
            )

            async for message in websocket:
                response = await self._process_incoming_text(client_address, message)
                if response:
                    await websocket.send(response)

        except Exception as e:
            logger.warning("Connection exception with client %s: %s", client_address, e)
        finally:
            logger.warning("Client disconnected: %s. Releasing controller and stopping motors.", client_address)
            self.connected_clients.discard(websocket)
            self.safety.unregister_client(client_address)

    async def _process_incoming_text(self, client_id: str, raw_text: str) -> str | None:
        """Processes a single incoming message from a client."""
        try:
            data = parse_message(raw_text)
        except ProtocolError as pe:
            logger.warning("Rejected invalid message from %s: %s", client_id, pe)
            return create_error(str(pe))

        msg_type = data.get("type")

        # 1. PING -> PONG
        if msg_type == TYPE_PING:
            client_ts = data.get("timestamp")
            return create_pong(client_ts)

        # 2. EMERGENCY STOP
        if msg_type == TYPE_STOP:
            command_id = str(data.get("command_id", ""))
            self.safety.trigger_stop(reason=f"Explicit STOP received from {client_id}")
            return create_ack(command_id=command_id, status="ok", reason="Robot halted immediately")

        # 3. MOVEMENT COMMAND (cmd_vel)
        if msg_type == TYPE_CMD_VEL:
            try:
                linear_x, angular_z, cmd_id = validate_and_clamp_cmd_vel(
                    data,
                    max_linear=self.safety.max_linear,
                    max_angular=self.safety.max_angular,
                )
            except ProtocolError as pe:
                return create_error(str(pe), command_id=data.get("command_id"))

            success, reason = await self.safety.process_cmd_vel(client_id, linear_x, angular_z)
            status_str = "ok" if success else "rejected"
            return create_ack(command_id=cmd_id, status=status_str, reason=reason)

        return create_error(f"Unknown message type '{msg_type}'")

    async def _telemetry_broadcaster(self) -> None:
        """Periodically streams telemetry updates to all active clients."""
        while self._running:
            try:
                await asyncio.sleep(config.TELEMETRY_INTERVAL_SEC)
                if not self.connected_clients:
                    continue

                telemetry = self.adapter.get_telemetry()
                msg = create_telemetry(
                    linear_velocity=telemetry["linear_velocity"],
                    angular_velocity=telemetry["angular_velocity"],
                    battery_voltage=telemetry["battery_voltage"],
                    is_stopped=not self.safety.is_moving,
                    has_active_lease=self.safety.has_active_lease(),
                    active_client=self.safety.active_client_id,
                )

                # Broadcast to all connected clients
                disconnected = set()
                for ws in self.connected_clients:
                    try:
                        await ws.send(msg)
                    except Exception:
                        disconnected.add(ws)

                for ws in disconnected:
                    self.connected_clients.discard(ws)

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error("Error in telemetry broadcaster: %s", e)


async def _async_main(args):
    server = RobotBridgeServer(
        host=args.host,
        port=args.port,
        max_linear=args.max_linear,
        max_angular=args.max_angular,
    )
    await server.start()
    try:
        while True:
            await asyncio.sleep(1)
    except asyncio.CancelledError:
        pass
    finally:
        await server.stop()


def main():
    parser = argparse.ArgumentParser(description="TurtleBot 3 Android WebSocket Bridge")
    parser.add_argument("--host", default=config.DEFAULT_HOST, help="Host network interface (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=config.DEFAULT_PORT, help="Port to listen on (default: 8765)")
    parser.add_argument("--max-linear", type=float, default=config.DEFAULT_MAX_LINEAR_VELOCITY, help="Max linear speed m/s")
    parser.add_argument("--max-angular", type=float, default=config.DEFAULT_MAX_ANGULAR_VELOCITY, help="Max angular speed rad/s")
    args = parser.parse_args()

    try:
        asyncio.run(_async_main(args))
    except KeyboardInterrupt:
        logger.info("Keyboard interrupt, exiting...")


if __name__ == "__main__":
    main()
