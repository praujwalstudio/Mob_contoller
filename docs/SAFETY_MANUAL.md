# TURTLEBOT 3 SAFETY MANUAL & HARDWARE PROTECTION

> [!CAUTION]
> **CRITICAL HARDWARE PROTECTION POLICY**
> A physical robot operates in the real world with kinetic energy, moving motors, delicate sensors (LiDAR), and expensive electronics (OpenCR 1.0, Raspberry Pi 3, Dynamixel servomotors). Uncontrolled movement can cause gear stripping, collision damage, or cable snagging.
> **Follow this protocol strictly before running normal operation.**

---

## 1. Elevated Test Stand Procedure (Zero-Risk Initial Verification)

Before placing the robot on the floor:
1. **Elevate the Robot**: Place the TurtleBot 3 Burger on an elevated stand or block so both drive wheels are suspended 1–2 inches above the table surface and can spin freely without touching anything.
2. **LiDAR & Wire Clearance**: Ensure no loose jumper wires, battery cables, or power cords are anywhere near the drive wheels or LiDAR belt.
3. **Power On Robot**: Turn on the OpenCR power switch and wait for the Raspberry Pi 3 to boot.
4. **Connect Phone**: Connect your Vivo Y28 5G to the TurtleBot Wi-Fi Hotspot.
5. **Initial Link Verification**:
   - Tap **CONNECT** in the TurtleBot Controller app.
   - Verify that the connection chip turns green: **CONNECTED**.
   - Verify that the motors do **not** move. (The robot must always start in a stopped state).
6. **Elevated Motion Testing**:
   - Press and hold **FORWARD**: Verify both wheels rotate forward slowly.
   - Release **FORWARD**: Verify both wheels stop spinning immediately (< 0.2s).
   - Press and hold **LEFT**: Verify wheels spin in opposite directions for turning.
   - Release: Verify immediate halt.
   - Tap **STOP**: Verify immediate software E-Stop trigger.
7. **Floor Placement**: Only place the robot on the floor after all elevated tests pass with 100% reliability.

---

## 2. Speed Limits & Boundary Controls

### Hardware Maximums vs Conservative Operating Speeds
| Parameter | Dynamixel Hardware Limit | Default Application Limit | Conservative Safe Crawl |
|---|---|---|---|
| **Linear Velocity** | `0.22 m/s` (~0.8 km/h) | `0.08 m/s` | `0.05 m/s` |
| **Angular Velocity** | `2.84 rad/s` (~162°/s) | `0.60 rad/s` (~34°/s) | `0.35 rad/s` |

- Keep speeds set to **0.05 – 0.08 m/s** for initial indoor testing.
- The Raspberry Pi bridge strictly enforces hardware maximums: even if a client sends excessive speed, the server clamps it to safe limits before publishing to `/cmd_vel`.

---

## 3. Fail-Safe Watchdogs & Multi-Layer Protection

The system implements 5 independent safety layers:

1. **Dead-Man Switch (Continuous Touch)**:
   - Direction buttons require a continuous finger press.
   - Every 120ms, the app renews the motion command.
   - The instant you lift your finger, an explicit STOP command is dispatched.
2. **Robot-Side Watchdog (400ms Command Lease)**:
   - The Raspberry Pi 3 evaluates incoming command freshness every 50ms.
   - If no fresh command is received within **400ms** (due to Wi-Fi drop, app crash, or lag), the bridge automatically halts the motors and publishes 3 consecutive zero-velocity bursts (`linear_x = 0.0, angular_z = 0.0`).
3. **Client Disconnect Hook**:
   - If the WebSocket closes, drops, or encounters a socket error, the bridge halts the motors immediately.
4. **App Lifecycle Protection**:
   - If your phone receives a phone call, is locked, or the app is minimized, the Android activity immediately dispatches an emergency stop.
5. **Multi-Client Control Lock**:
   - Only ONE phone client can hold the active movement lock. Secondary clients are rejected from sending motion commands.

---

## 4. Emergency Procedures

- **Software E-Stop**: Tap the large, red **STOP** button in the center of the directional pad.
- **Physical E-Stop**: The OpenCR controller has a physical power switch and reset button on the side. If anything unexpected occurs, flick the power switch to OFF immediately.
- **Battery Protection**: Never operate the robot if the 3S LiPo battery drops below **11.1V** (3.7V per cell). The app displays a red warning when battery voltage is critical.
