"""
TurtleBot 3 ROS Adapter Layer
Provides unified interface for both ROS 1 (Noetic) and ROS 2 (Humble/Foxy),
with an automated detector and a standalone Mock adapter for offline testing.
"""

import logging
import threading
import time
from typing import Dict, Any, Optional
from config import (
    NODE_NAME,
    ROS_BATTERY_TOPIC,
    ROS_CMD_VEL_TOPIC,
    ROS_ODOM_TOPIC,
)

logger = logging.getLogger("TurtleBotROSAdapter")


class BaseROSAdapter:
    """Abstract base class for ROS communication."""

    def __init__(self) -> None:
        self.latest_linear_x = 0.0
        self.latest_angular_z = 0.0
        self.battery_voltage = 12.0
        self.last_telemetry_time = time.time()

    def publish_cmd_vel(self, linear_x: float, angular_z: float) -> None:
        raise NotImplementedError

    def publish_zero(self) -> None:
        self.publish_cmd_vel(0.0, 0.0)

    def get_telemetry(self) -> Dict[str, Any]:
        return {
            "linear_velocity": self.latest_linear_x,
            "angular_velocity": self.latest_angular_z,
            "battery_voltage": self.battery_voltage,
            "timestamp": time.time(),
        }

    def shutdown(self) -> None:
        pass


class ROS2Adapter(BaseROSAdapter):
    """ROS 2 implementation using rclpy."""

    def __init__(self) -> None:
        super().__init__()
        import rclpy
        from rclpy.node import Node
        from geometry_msgs.msg import Twist
        from sensor_msgs.msg import BatteryState
        from nav_msgs.msg import Odometry

        self._rclpy = rclpy
        if not rclpy.ok():
            rclpy.init()

        self.node = Node(NODE_NAME)
        self.publisher = self.node.create_publisher(Twist, ROS_CMD_VEL_TOPIC, 10)
        self._twist_cls = Twist

        # Subscriptions for real telemetry
        self.node.create_subscription(BatteryState, ROS_BATTERY_TOPIC, self._battery_cb, 10)
        self.node.create_subscription(Odometry, ROS_ODOM_TOPIC, self._odom_cb, 10)

        self._spin_thread = threading.Thread(target=self._spin, daemon=True)
        self._spin_thread.start()
        logger.info("ROS 2 (rclpy) Adapter initialized on topic '%s'.", ROS_CMD_VEL_TOPIC)

    def _spin(self) -> None:
        try:
            self._rclpy.spin(self.node)
        except Exception as e:
            logger.debug("ROS 2 spin finished: %s", e)

    def _battery_cb(self, msg) -> None:
        if hasattr(msg, "voltage") and msg.voltage > 0:
            self.battery_voltage = float(msg.voltage)

    def _odom_cb(self, msg) -> None:
        if hasattr(msg, "twist") and hasattr(msg.twist, "twist"):
            self.latest_linear_x = float(msg.twist.twist.linear.x)
            self.latest_angular_z = float(msg.twist.twist.angular.z)

    def publish_cmd_vel(self, linear_x: float, angular_z: float) -> None:
        msg = self._twist_cls()
        msg.linear.x = float(linear_x)
        msg.angular.z = float(angular_z)
        self.publisher.publish(msg)
        self.latest_linear_x = linear_x
        self.latest_angular_z = angular_z

    def shutdown(self) -> None:
        self.publish_zero()
        try:
            self.node.destroy_node()
            if self._rclpy.ok():
                self._rclpy.shutdown()
        except Exception as e:
            logger.warning("Error shutting down ROS 2 node: %s", e)


class ROS1Adapter(BaseROSAdapter):
    """ROS 1 implementation using rospy."""

    def __init__(self) -> None:
        super().__init__()
        import rospy
        from geometry_msgs.msg import Twist
        from sensor_msgs.msg import BatteryState
        from nav_msgs.msg import Odometry

        self._rospy = rospy
        rospy.init_node(NODE_NAME, anonymous=True, disable_signals=True)
        self.publisher = rospy.Publisher(ROS_CMD_VEL_TOPIC, Twist, queue_size=10)
        self._twist_cls = Twist

        # Subscriptions for real telemetry
        rospy.Subscriber(ROS_BATTERY_TOPIC, BatteryState, self._battery_cb)
        rospy.Subscriber(ROS_ODOM_TOPIC, Odometry, self._odom_cb)

        logger.info("ROS 1 (rospy) Adapter initialized on topic '%s'.", ROS_CMD_VEL_TOPIC)

    def _battery_cb(self, msg) -> None:
        if hasattr(msg, "voltage") and msg.voltage > 0:
            self.battery_voltage = float(msg.voltage)

    def _odom_cb(self, msg) -> None:
        if hasattr(msg, "twist") and hasattr(msg.twist, "twist"):
            self.latest_linear_x = float(msg.twist.twist.linear.x)
            self.latest_angular_z = float(msg.twist.twist.angular.z)

    def publish_cmd_vel(self, linear_x: float, angular_z: float) -> None:
        msg = self._twist_cls()
        msg.linear.x = float(linear_x)
        msg.angular.z = float(angular_z)
        self.publisher.publish(msg)
        self.latest_linear_x = linear_x
        self.latest_angular_z = angular_z

    def shutdown(self) -> None:
        self.publish_zero()


class MockROSAdapter(BaseROSAdapter):
    """Mock ROS Adapter for testing on systems without ROS installed."""

    def __init__(self) -> None:
        super().__init__()
        self.published_history: list[tuple[float, float, float]] = []
        logger.info("Mock ROS Adapter initialized (dry-run / offline mode).")

    def publish_cmd_vel(self, linear_x: float, angular_z: float) -> None:
        self.latest_linear_x = linear_x
        self.latest_angular_z = angular_z
        self.published_history.append((linear_x, angular_z, time.time()))
        logger.debug("Mock ROS Published: linear_x=%.3f, angular_z=%.3f", linear_x, angular_z)

    def shutdown(self) -> None:
        self.publish_zero()
        logger.info("Mock ROS Adapter shut down safely.")


def detect_and_create_adapter() -> BaseROSAdapter:
    """
    Automatically detects whether ROS 2 or ROS 1 is installed and returns the
    appropriate adapter. Falls back to MockROSAdapter if neither is present.
    """
    # 1. Check for ROS 2
    try:
        import rclpy
        from geometry_msgs.msg import Twist
        logger.info("Auto-detection: Found ROS 2 (rclpy). Using ROS2Adapter.")
        return ROS2Adapter()
    except (ImportError, Exception) as e_ros2:
        logger.debug("ROS 2 not detected: %s", e_ros2)

    # 2. Check for ROS 1
    try:
        import rospy
        from geometry_msgs.msg import Twist
        logger.info("Auto-detection: Found ROS 1 (rospy). Using ROS1Adapter.")
        return ROS1Adapter()
    except (ImportError, Exception) as e_ros1:
        logger.debug("ROS 1 not detected: %s", e_ros1)

    # 3. Fallback to Mock
    logger.warning("Auto-detection: Neither ROS 1 nor ROS 2 found. Using MockROSAdapter.")
    return MockROSAdapter()
