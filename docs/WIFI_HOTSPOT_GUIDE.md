# TURTLEBOT 3 STANDALONE WI-FI HOTSPOT GUIDE

This guide explains how to configure the Raspberry Pi 3 onboard computer on your TurtleBot 3 Burger to operate as a **standalone Wi-Fi Access Point (Hotspot)**.

Operating in standalone Hotspot mode allows your **Vivo Y28 5G** Android smartphone to connect directly to the robot anywhere (in a robotics lab, field, outdoors, or home) **without needing a Wi-Fi router or external internet connection**.

---

## 1. Network Architecture

```
+------------------------------------+
|  Vivo Y28 5G Android Smartphone    |
|  IP: 192.168.4.15 (DHCP Client)    |
+-----------------+------------------+
                  |
         Direct Wi-Fi Link
          (SSID: TurtleBot3)
                  |
+-----------------v------------------+
|  Raspberry Pi 3 (Onboard Computer) |
|  Internal IP: 192.168.4.1 (Gateway)|
|  WebSocket Port: 8765              |
+-----------------+------------------+
                  |
              USB Serial
                  |
+-----------------v------------------+
|  OpenCR 1.0 Motor Controller       |
+------------------------------------+
```

---

## 2. Setting Up Wi-Fi Hotspot on Raspberry Pi 3 (Ubuntu / NetworkManager)

Most Ubuntu/Raspberry Pi OS setups use `NetworkManager` (`nmcli`). This is the cleanest and most reliable way to create a persistent hotspot.

### Step 1: Create the Hotspot Connection
SSH or connect a monitor/keyboard to your Raspberry Pi, then run:
```bash
sudo nmcli dev wifi hotspot ifname wlan0 ssid "TurtleBot3" password "turtlebot1234"
```

### Step 2: Configure Static IP for the Gateway
Set the Pi's Wi-Fi hotspot address to `192.168.4.1`:
```bash
sudo nmcli connection modify Hotspot ipv4.addresses 192.168.4.1/24
sudo nmcli connection modify Hotspot ipv4.method shared
sudo nmcli connection modify Hotspot connection.autoconnect yes
sudo nmcli connection up Hotspot
```

### Step 3: Verify Hotspot is Active
Run:
```bash
ip addr show wlan0
```
You should see:
```
inet 192.168.4.1/24 brd 192.168.4.255 scope global wlan0
```

---

## 3. Alternative Method: hostapd + dnsmasq (For Older Raspbian / Ubuntu)

If your Raspberry Pi does not use NetworkManager, configure `hostapd` and `dnsmasq`:

1. Install packages:
   ```bash
   sudo apt-get update
   sudo apt-get install -y hostapd dnsmasq
   ```
2. Configure static IP in `/etc/dhcpcd.conf`:
   ```ini
   interface wlan0
       static ip_address=192.168.4.1/24
       nohook wpa_supplicant
   ```
3. Configure DHCP in `/etc/dnsmasq.conf`:
   ```ini
   interface=wlan0
   dhcp-range=192.168.4.10,192.168.4.50,255.255.255.0,24h
   ```
4. Configure `/etc/hostapd/hostapd.conf`:
   ```ini
   interface=wlan0
   driver=nl80211
   ssid=TurtleBot3
   hw_mode=g
   channel=7
   wmm_enabled=0
   macaddr_acl=0
   auth_algs=1
   wpa=2
   wpa_passphrase=turtlebot1234
   wpa_key_mgmt=WPA-PSK
   rsn_pairwise=CCMP
   ```
5. Enable and start services:
   ```bash
   sudo systemctl unmask hostapd
   sudo systemctl enable hostapd
   sudo systemctl restart hostapd
   sudo systemctl restart dnsmasq
   ```

---

## 4. Connecting Your Vivo Y28 5G Phone

1. On your Vivo Y28 5G, go to **Settings > Wi-Fi**.
2. Select **TurtleBot3** from the available Wi-Fi networks.
3. Enter the password: `turtlebot1234`.
4. If Android displays a prompt saying *"This network has no internet access. Stay connected?"*, select **Yes / Always Stay Connected**.
5. Launch the **TurtleBot Controller** app.
6. Under **Settings**, verify IP is set to `192.168.4.1` and Port is `8765`.
7. Tap **CONNECT**. You are now linked directly to your robot!
