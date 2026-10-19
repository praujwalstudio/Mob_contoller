"""
TurtleBot 3 Robot Bridge Configuration
Defines server networking, ROS parameters, and strict hardware safety limits.
"""

# Networking
DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8765

# ROS Configuration
ROS_CMD_VEL_TOPIC = "/cmd_vel"
ROS_ODOM_TOPIC = "/odom"
ROS_BATTERY_TOPIC = "/battery_state"
NODE_NAME = "turtlebot3_android_bridge"

# ==============================================================================
# CRITICAL SAFETY LIMITS (TurtleBot 3 Burger Hardware Protection)
# ==============================================================================

# Hardware maximums for TurtleBot 3 Burger (Dynamixel XL430-W250)
HARD_LIMIT_MAX_LINEAR_VELOCITY = 0.22   # m/s (Absolute physical maximum)
HARD_LIMIT_MAX_ANGULAR_VELOCITY = 2.84  # rad/s (Absolute physical maximum)

# Conservative defaults for safe indoor operation
DEFAULT_MAX_LINEAR_VELOCITY = 0.12      # m/s (~55% of max speed for safety)
DEFAULT_MAX_ANGULAR_VELOCITY = 1.00     # rad/s (~35% of max speed for smooth turns)

# Command lease watchdog timeout in seconds.
# If no new motion command arrives within this duration, motors are stopped immediately.
COMMAND_LEASE_TIMEOUT_SEC = 0.40        # 400 milliseconds

# Watchdog inspection frequency (seconds)
WATCHDOG_CHECK_INTERVAL_SEC = 0.05      # 50 milliseconds (20 checks/second)

# Number of consecutive zero-velocity commands published to clear ROS queue upon stop
STOP_BURST_COUNT = 3

# Telemetry broadcast interval in seconds
TELEMETRY_INTERVAL_SEC = 0.10           # 100 milliseconds (10 Hz)
