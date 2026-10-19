# TurtleBot 3 WebSocket Bridge

A dual-compatible (ROS 1 & ROS 2) asynchronous WebSocket bridge for the TurtleBot 3 Burger onboard computer (Raspberry Pi 3).

## Features
- **Dual ROS Support**: Automatically detects whether ROS 1 (`rospy`) or ROS 2 (`rclpy`) is active.
- **Fail-Safe Watchdog**: Enforces an aggressive 400ms command lease. If no command arrives within 400ms, the bridge automatically publishes zero velocity (`Twist(0, 0)`).
- **Client Disconnect Halt**: Disconnecting or socket drop immediately stops all physical robot movement.
- **Multi-Client Protection**: Locks active movement control to the primary client session; other clients can observe telemetry but cannot issue conflicting movement commands.
- **Velocity Clamping**: Strictly enforces hardware maximums (linear: `0.22` m/s, angular: `2.84` rad/s).
- **Mock Mode**: Includes an offline simulation mode with zero dependencies for PC testing.

## Quickstart

### 1. Installation on Raspberry Pi 3
```bash
cd ~
git clone <this-repo>
cd robotcontoller/turtlebot_bridge
pip3 install -r requirements.txt
```

### 2. Running on the Robot
Make sure your TurtleBot base is running:
- **For ROS 1 Noetic**:
  ```bash
  roslaunch turtlebot3_bringup turtlebot3_robot.launch
  ```
- **For ROS 2 Humble/Foxy**:
  ```bash
  ros2 launch turtlebot3_bringup robot.launch.py
  ```

Then in a new terminal, launch the bridge:
```bash
python3 server.py --host 0.0.0.0 --port 8765
```

### 3. Running Standalone Mock on PC (Testing Mode)
```bash
python mock_bridge.py
```

### 4. Running Tests
```bash
python -m unittest discover -s tests -v
```
