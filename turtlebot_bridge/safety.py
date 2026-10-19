"""
TurtleBot 3 Safety & Watchdog Engine
Guarantees fail-safe robot operation, command leasing, speed clamping, and client ownership locks.
"""

import asyncio
import logging
import time
from typing import Callable, Optional
from config import (
    COMMAND_LEASE_TIMEOUT_SEC,
    DEFAULT_MAX_ANGULAR_VELOCITY,
    DEFAULT_MAX_LINEAR_VELOCITY,
    HARD_LIMIT_MAX_ANGULAR_VELOCITY,
    HARD_LIMIT_MAX_LINEAR_VELOCITY,
    STOP_BURST_COUNT,
    WATCHDOG_CHECK_INTERVAL_SEC,
)

logger = logging.getLogger("TurtleBotSafety")


class SafetyWatchdog:
    """
    Manages client ownership, command leasing, and watchdog monitoring to prevent
    uncontrolled physical robot motion under any network failure or client stall.
    """

    def __init__(
        self,
        publish_zero_fn: Callable[[], None],
        publish_twist_fn: Callable[[float, float], None],
        max_linear: float = DEFAULT_MAX_LINEAR_VELOCITY,
        max_angular: float = DEFAULT_MAX_ANGULAR_VELOCITY,
        lease_timeout: float = COMMAND_LEASE_TIMEOUT_SEC,
    ):
        self._publish_zero = publish_zero_fn
        self._publish_twist = publish_twist_fn

        # Safety limits: hard clamped to hardware maximums
        self.max_linear = min(max_linear, HARD_LIMIT_MAX_LINEAR_VELOCITY)
        self.max_angular = min(max_angular, HARD_LIMIT_MAX_ANGULAR_VELOCITY)
        self.lease_timeout = lease_timeout

        # Internal state
        self._active_client_id: Optional[str] = None
        self._last_command_time: float = 0.0
        self._current_linear: float = 0.0
        self._current_angular: float = 0.0
        self._is_moving: bool = False
        self._emergency_stopped: bool = False
        self._running: bool = False
        self._watchdog_task: Optional[asyncio.Task] = None
        self._lock = asyncio.Lock()

    @property
    def is_moving(self) -> bool:
        return self._is_moving

    @property
    def emergency_stopped(self) -> bool:
        return self._emergency_stopped

    @property
    def active_client_id(self) -> Optional[str]:
        return self._active_client_id

    @property
    def current_velocities(self) -> tuple[float, float]:
        return (self._current_linear, self._current_angular)

    def has_active_lease(self) -> bool:
        if not self._is_moving:
            return False
        return (time.time() - self._last_command_time) < self.lease_timeout

    async def start(self) -> None:
        """Starts the background watchdog evaluation loop."""
        self._running = True
        self.trigger_stop(reason="Initial startup in stopped state")
        self._watchdog_task = asyncio.create_task(self._watchdog_loop())
        logger.info("Safety watchdog loop started (lease timeout = %.2fs)", self.lease_timeout)

    async def stop_watchdog(self) -> None:
        """Stops the watchdog background task and forces a robot stop."""
        self._running = False
        if self._watchdog_task and not self._watchdog_task.done():
            self._watchdog_task.cancel()
            try:
                await self._watchdog_task
            except asyncio.CancelledError:
                pass
        self.trigger_stop(reason="Bridge shutdown")

    def register_client(self, client_id: str) -> bool:
        """Registers a client as the primary controller if no client currently owns it."""
        if self._active_client_id is None:
            self._active_client_id = client_id
            logger.info("Client '%s' acquired active robot control ownership.", client_id)
            return True
        elif self._active_client_id == client_id:
            return True
        else:
            logger.warning(
                "Client '%s' requested control, but robot is already owned by '%s'.",
                client_id, self._active_client_id
            )
            return False

    def unregister_client(self, client_id: str) -> None:
        """Unregisters the controlling client and triggers an immediate safe stop."""
        if self._active_client_id == client_id:
            logger.warning("Active controlling client '%s' disconnected. Halting robot immediately.", client_id)
            self._active_client_id = None
            self.trigger_stop(reason=f"Client {client_id} disconnected")

    async def process_cmd_vel(
        self, client_id: str, linear_x: float, angular_z: float
    ) -> tuple[bool, str]:
        """
        Processes an incoming movement command with ownership and speed verification.
        Returns (success, reason).
        """
        async with self._lock:
            # Check ownership
            if self._active_client_id is not None and self._active_client_id != client_id:
                return False, f"Control locked by active client: {self._active_client_id}"

            # Acquire ownership if free
            if self._active_client_id is None:
                self._active_client_id = client_id

            # Clamping velocities strictly within safe bounds
            linear_clamped = max(-self.max_linear, min(self.max_linear, linear_x))
            angular_clamped = max(-self.max_angular, min(self.max_angular, angular_z))

            # Renew command lease
            self._last_command_time = time.time()
            self._current_linear = linear_clamped
            self._current_angular = angular_clamped
            self._emergency_stopped = False

            if abs(linear_clamped) < 1e-4 and abs(angular_clamped) < 1e-4:
                self._is_moving = False
                self._publish_zero()
            else:
                self._is_moving = True
                self._publish_twist(linear_clamped, angular_clamped)

            return True, "Command accepted and lease renewed"

    def trigger_stop(self, reason: str = "User stop request") -> None:
        """
        Forces immediate zero-velocity publication and resets movement state.
        Dispatches multiple consecutive zero-velocity commands to ensure clearing
        any downstream buffers in the OpenCR / ROS controller.
        """
        self._current_linear = 0.0
        self._current_angular = 0.0
        self._is_moving = False
        self._emergency_stopped = True
        self._last_command_time = 0.0

        for _ in range(STOP_BURST_COUNT):
            self._publish_zero()

        logger.info("SAFETY STOP TRIGGERED: %s (Motors halted with %d zero-bursts)", reason, STOP_BURST_COUNT)

    async def _watchdog_loop(self) -> None:
        """
        Periodically checks lease expiration.
        If no command has been received within COMMAND_LEASE_TIMEOUT_SEC while moving,
        halts the robot automatically.
        """
        while self._running:
            try:
                await asyncio.sleep(WATCHDOG_CHECK_INTERVAL_SEC)
                if self._is_moving:
                    elapsed = time.time() - self._last_command_time
                    if elapsed > self.lease_timeout:
                        self.trigger_stop(
                            reason=f"Command lease expired (elapsed {elapsed*1000:.1f}ms > limit {self.lease_timeout*1000:.1f}ms)"
                        )
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error("Unexpected error in safety watchdog loop: %s", e)
                self.trigger_stop(reason=f"Watchdog exception: {e}")
