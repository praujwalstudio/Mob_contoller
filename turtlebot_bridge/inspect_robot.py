#!/usr/bin/env python3
"""
TurtleBot 3 Raspberry Pi Environment Inspector
A zero-dependency diagnostic tool to inspect the onboard Raspberry Pi 3 software setup:
detects Operating System, ROS 1 / ROS 2 distribution, OpenCR USB hardware, and network IPs.
"""

import os
import platform
import socket
import subprocess
import sys


def print_header(title: str) -> None:
    print("\n" + "=" * 65)
    print(f"  {title.upper()}")
    print("=" * 65)


def check_os() -> None:
    print_header("1. Operating System Details")
    print(f"  Platform:      {platform.platform()}")
    print(f"  Architecture:  {platform.machine()}")
    print(f"  Python:        {sys.version.split()[0]}")

    # Check /etc/os-release
    if os.path.exists("/etc/os-release"):
        with open("/etc/os-release") as f:
            for line in f:
                if line.startswith(("PRETTY_NAME=", "NAME=", "VERSION=")):
                    k, v = line.strip().split("=", 1)
                    print(f"  {k:14} {v.strip('\"')}")


def check_ros() -> None:
    print_header("2. Robotics Middleware (ROS Environment)")
    ros_distro = os.environ.get("ROS_DISTRO")
    ros_version = os.environ.get("ROS_VERSION")
    tb3_model = os.environ.get("TURTLEBOT3_MODEL")

    print(f"  ROS_DISTRO:        {ros_distro if ros_distro else '[NOT SET]'}")
    print(f"  ROS_VERSION:       {ros_version if ros_version else '[NOT SET]'}")
    print(f"  TURTLEBOT3_MODEL:  {tb3_model if tb3_model else '[NOT SET - recommend: export TURTLEBOT3_MODEL=burger]'}")

    # Test ROS 2 (rclpy)
    try:
        import rclpy
        from geometry_msgs.msg import Twist
        print("  [SUCCESS] ROS 2 Python bindings (rclpy) detected and functional.")
        detected_ros = "ROS 2"
    except ImportError:
        print("  [INFO] ROS 2 (rclpy) is not in current Python path.")
        detected_ros = None

    # Test ROS 1 (rospy)
    try:
        import rospy
        from geometry_msgs.msg import Twist
        print("  [SUCCESS] ROS 1 Python bindings (rospy) detected and functional.")
        if not detected_ros:
            detected_ros = "ROS 1"
    except ImportError:
        print("  [INFO] ROS 1 (rospy) is not in current Python path.")

    if not detected_ros:
        print("  [ATTENTION] Neither ROS 1 nor ROS 2 is sourced in this terminal session.")
        print("  Make sure to run: source /opt/ros/<your-distro>/setup.bash")


def check_hardware() -> None:
    print_header("3. Motor Controller Hardware (OpenCR)")
    ports = ["/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyUSB0", "/dev/ttyUSB1"]
    found_any = False
    for p in ports:
        if os.path.exists(p):
            found_any = True
            is_rw = os.access(p, os.R_OK | os.W_OK)
            rw_status = "Read/Write OK" if is_rw else "PERMISSION DENIED (run: sudo chmod 666 " + p + ")"
            print(f"  [FOUND] Serial Port: {p} -> {rw_status}")

    if not found_any:
        print("  [WARNING] OpenCR micro-USB port (/dev/ttyACM*) not detected.")
        print("  Check: 1) OpenCR USB cable plugged into Raspberry Pi 3.")
        print("         2) OpenCR power switch is turned ON.")


def check_network() -> None:
    print_header("4. Network Interfaces & IP Addresses")
    hostname = socket.gethostname()
    print(f"  Hostname: {hostname}")

    try:
        res = subprocess.run(["hostname", "-I"], stdout=subprocess.PIPE, text=True, timeout=3)
        ips = res.stdout.strip().split()
        if ips:
            print("  Active IP Addresses on this Raspberry Pi:")
            for ip in ips:
                print(f"    -> {ip}")
            print(f"\n  Use one of the above IP addresses in your mobile app Connection tab.")
        else:
            print("  No active IP addresses found. Check Wi-Fi connection.")
    except Exception:
        pass

    # Check if port 8765 is active
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1)
        res = s.connect_ex(("127.0.0.1", 8765))
        if res == 0:
            print("\n  [ACTIVE] WebSocket Bridge is CURRENTLY RUNNING on port 8765.")
        else:
            print("\n  [READY] Port 8765 is free and ready for server.py.")
        s.close()
    except Exception:
        pass


def print_recommendation() -> None:
    print_header("5. How to Launch Bridge on this Robot")
    print("  Run this exact command to start the WebSocket bridge:")
    print("\n    python3 server.py --host 0.0.0.0 --port 8765\n")
    print("=" * 65)


def main():
    print("=" * 65)
    print("   TURTLEBOT 3 RASPBERRY PI 3 ENVIRONMENT INSPECTION TOOL")
    print("=" * 65)
    check_os()
    check_ros()
    check_hardware()
    check_network()
    print_recommendation()


if __name__ == "__main__":
    main()
