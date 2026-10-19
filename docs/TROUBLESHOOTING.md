# TURTLEBOT 3 CONTROLLER TROUBLESHOOTING GUIDE

Common issues and verified solutions when connecting the Vivo Y28 5G to the TurtleBot 3.

---

## 1. Connection Issues

### Issue: App shows "Connection Error" or times out when connecting
- **Check 1: Wi-Fi connection**: On your Vivo Y28 5G, verify that you are connected to the `TurtleBot3` Wi-Fi hotspot.
- **Check 2: Wi-Fi Gateway IP**: By default, the Pi Hotspot gateway is `192.168.4.1`. If your Pi assigned a different IP range (e.g. `10.42.0.1` in Ubuntu NetworkManager), update the IP address in the App's **Settings** tab.
- **Check 3: Bridge server running**: On the Pi, run `sudo systemctl status turtlebot-bridge.service` or check `ps aux | grep server.py`.
- **Check 4: Port accessibility**: Ensure port `8765` is not blocked by a firewall:
  ```bash
  sudo ufw allow 8765/tcp
  ```

---

## 2. Motor Movement Issues

### Issue: Connected, but the robot does not move when pressing directional controls
- **Check 1: OpenCR USB power**: The OpenCR controller must have its power switch in the **ON** position. The Dynamixel motors are powered by the 11.1V battery via OpenCR, not by the Pi's USB port.
- **Check 2: OpenCR permissions**: Ensure the user running the bridge has permission to access `/dev/ttyACM0`:
  ```bash
  sudo chmod 666 /dev/ttyACM0
  ```
- **Check 3: Base controller running**: Verify that the ROS base node is running and publishing/listening to `/cmd_vel`:
  ```bash
  # Check active topics:
  rostopic list        # (ROS 1)
  ros2 topic list      # (ROS 2)
  
  # Echo command velocity while pressing Forward on the phone:
  rostopic echo /cmd_vel     # (ROS 1)
  ros2 topic echo /cmd_vel   # (ROS 2)
  ```

---

## 3. Immediate Stopping / Stuttering Motion

### Issue: Robot moves forward for a fraction of a second and immediately halts
- **Cause**: Command lease expiration.
- **Explanation**: The robot bridge enforces a 400ms command lease for safety. If Wi-Fi signal drops or packet transmission latency exceeds 400ms, the robot halts to prevent uncontrolled runaways.
- **Solution**:
  - Keep your Vivo Y28 5G within 10 meters of the robot for optimal direct Wi-Fi reception.
  - On the phone, disable any aggressive power-saving or "battery optimization" for the TurtleBot Controller app that might throttle background Wi-Fi transmission.

---

## 4. Battery Low Warning

### Issue: App shows Battery Voltage in RED (< 11.1V)
- **Explanation**: The TurtleBot 3 uses a 3-cell Lithium-Polymer (LiPo) battery (11.1V nominal, 12.6V fully charged, 10.5V absolute minimum cutoff).
- **Action**: Immediately shut down the robot and recharge the LiPo battery using a balanced LiPo charger. Never discharge below 10.5V, as this can permanently damage LiPo cells.
