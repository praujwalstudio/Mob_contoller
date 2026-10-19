# TURTLEBOT 3 RASPBERRY PI 3 BRIDGE SETUP GUIDE

This guide details how to install, configure, and automate the **TurtleBot 3 WebSocket Bridge** service on your Raspberry Pi 3 onboard computer.

---

## 1. System Requirements
- Hardware: TurtleBot 3 Burger (Raspberry Pi 3 Model B/B+, OpenCR 1.0, Dynamixel XL430-W250)
- Operating System: Ubuntu 20.04 (ROS 1 Noetic or ROS 2 Foxy) or Ubuntu 22.04 (ROS 2 Humble)
- Python: Python 3.8+
- Network: Configured as a Wi-Fi Hotspot (see `docs/WIFI_HOTSPOT_GUIDE.md`)

---

## 2. Installing the Bridge Files on Raspberry Pi 3

Copy the `turtlebot_bridge` folder to your Raspberry Pi home directory:
```bash
# On your Raspberry Pi:
mkdir -p ~/turtlebot_ws
cd ~/turtlebot_ws

# If copying over SCP or USB drive:
# scp -r turtlebot_bridge pi@192.168.4.1:~/turtlebot_ws/
cd ~/turtlebot_ws/turtlebot_bridge

# Install Python dependencies
pip3 install -r requirements.txt
```

---

## 3. Verifying ROS Setup & OpenCR Connection

Ensure your OpenCR 1.0 controller is connected to the Raspberry Pi 3 via micro-USB.
Check that the USB serial device exists:
```bash
ls -l /dev/ttyACM*
```
You should see `/dev/ttyACM0`.

Grant read/write permissions to the serial device:
```bash
sudo usermod -aG dialout $USER
sudo chmod a+rw /dev/ttyACM0
```

### Launch the TurtleBot 3 Base Nodes

#### For ROS 1 (Noetic):
```bash
export TURTLEBOT3_MODEL=burger
roslaunch turtlebot3_bringup turtlebot3_robot.launch
```

#### For ROS 2 (Humble / Foxy):
```bash
export TURTLEBOT3_MODEL=burger
ros2 launch turtlebot3_bringup robot.launch.py
```

---

## 4. Testing the Bridge Manually

Open a separate terminal window on the Raspberry Pi:
```bash
cd ~/turtlebot_ws/turtlebot_bridge
python3 server.py --host 0.0.0.0 --port 8765
```

You should see:
```
[INFO] [TurtleBotROSAdapter] Auto-detection: Found ROS ...
[INFO] [TurtleBotSafety] SAFETY STOP TRIGGERED: Initial startup in stopped state
[INFO] [TurtleBotSafety] Safety watchdog loop started (lease timeout = 0.40s)
[INFO] [TurtleBotServer] Starting TurtleBot 3 WebSocket Bridge on ws://0.0.0.0:8765 ...
[INFO] [TurtleBotServer] Bridge is live and awaiting connections.
```

---

## 5. Setting Up Auto-Start as a Systemd Service

To ensure the robot controller bridge automatically starts whenever the Raspberry Pi boots:

1. Create a systemd service file:
   ```bash
   sudo nano /etc/systemd/system/turtlebot-bridge.service
   ```

2. Paste the following configuration (adjust `/home/ubuntu` to your username if different):
   ```ini
   [Unit]
   Description=TurtleBot 3 Android WebSocket Bridge
   After=network.target network-online.target roscore.service

   [Service]
   Type=simple
   User=ubuntu
   WorkingDirectory=/home/ubuntu/turtlebot_ws/turtlebot_bridge
   Environment="TURTLEBOT3_MODEL=burger"
   Environment="PYTHONUNBUFFERED=1"
   # Source ROS environment before running
   ExecStart=/bin/bash -c "source /opt/ros/$ROS_DISTRO/setup.bash && python3 server.py --host 0.0.0.0 --port 8765"
   Restart=always
   RestartSec=3

   [Install]
   WantedBy=multi-user.target
   ```

3. Reload systemd, enable, and start the service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable turtlebot-bridge.service
   sudo systemctl start turtlebot-bridge.service
   ```

4. Check the service status and logs:
   ```bash
   sudo systemctl status turtlebot-bridge.service
   journalctl -u turtlebot-bridge.service -f
   ```
