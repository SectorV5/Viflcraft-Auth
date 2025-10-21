# What Was Wrong and How It's Fixed

## ❌ The Problem

The original script had a **CRITICAL FLAW** that broke your entire VPS:

### What Went Wrong:

1. **WireGuard captured ALL traffic**
   - The WireGuard config had `AllowedIPs = 0.0.0.0/0` which told the host OS to route EVERYTHING through the VPN
   - This included SSH traffic, web server traffic, and all host communication
   - Result: **You couldn't access your VPS anymore**

2. **Firewall blocked host traffic**
   - Script flushed ALL firewall rules (`iptables -F FORWARD`)
   - Changed default FORWARD policy to DROP
   - This blocked legitimate host traffic forwarding

3. **Network configuration affected host**
   - Changed system-wide `/etc/sysctl.conf`
   - Could affect other services on the host

## ✅ The Fixes

### Fix 1: WireGuard Configuration (CRITICAL)

**Before (BROKEN):**
```ini
[Interface]
Address = 10.173.212.207/32
DNS = 10.128.0.1
# No Table parameter - used default routing table!

[Peer]
AllowedIPs = 0.0.0.0/0,::/0  # ← THIS BROKE EVERYTHING
```

**After (FIXED):**
```ini
[Interface]
Address = 10.173.212.207/32
Table = off  # ← CRITICAL: Don't modify main routing table
PostUp = ip route add 10.10.10.0/24 dev wg0  # ← Only route VM subnet
# No DNS configuration - host DNS unchanged

[Peer]
AllowedIPs = 0.0.0.0/0,::/0  # ← This now only applies to wg0 interface
```

**Key changes:**
- `Table = off` - Prevents WireGuard from modifying the host's main routing table
- `PostUp` adds route ONLY for VM subnet (10.10.10.0/24)
- No DNS configuration - host uses its own DNS
- Host's default route remains via eth0 (or whatever your main interface is)

### Fix 2: Firewall Rules

**Before (BROKEN):**
```bash
iptables -F FORWARD          # Deleted ALL forward rules
iptables -P FORWARD DROP     # Blocked all forwarding by default
iptables -t nat -F POSTROUTING  # Deleted all NAT rules
```

**After (FIXED):**
```bash
# Do NOT flush rules or change policies
# Only add specific VM rules
iptables -A INPUT -i vm-bridge -p udp --dport 67 -j ACCEPT  # DHCP only
iptables -A FORWARD -i vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT  # VM connections

# Main rules added by WireGuard PostUp:
# - Forward VM → wg0
# - NAT from 10.10.10.0/24 only
# - Block VM → non-VPN interfaces
```

**Key changes:**
- No flushing of existing rules
- No changing of default policies
- Only append VM-specific rules
- Preserve all host firewall configuration

### Fix 3: System Configuration

**Before (BROKEN):**
```bash
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf  # Modified main config
```

**After (FIXED):**
```bash
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.d/99-vm-forwarding.conf  # Separate file
```

**Key changes:**
- Uses dedicated config file in `/etc/sysctl.d/`
- Doesn't touch `/etc/sysctl.conf`
- Easier to remove cleanly

## 🔍 How Traffic Flows Now (CORRECT)

### Host Traffic (Your VPS):
```
Host → eth0 → Internet (Hetzner network)
  ↑
  └─ SSH, websites, everything on the host works normally
```

### VM Traffic (Your Friend's VM):
```
VM → vm-bridge → iptables check → wg0 (VPN) → Internet
  ↑                    ↑            ↑
  │                    │            └─ All VM traffic exits via VPN IP
  │                    └─ Kill switch: blocks if wg0 is down
  └─ Isolated VM (10.10.10.0/24)
```

### If VPN Fails:
```
Host → eth0 → Internet ✓ (Still works!)
VM → vm-bridge → iptables → REJECTED ✗ (Kill switch active)
```

## 🧪 How to Verify It's Fixed

### Test 1: Host Routing
```bash
# Check default route (should NOT be wg0)
ip route show default

# Should show something like:
# default via 172.31.1.1 dev eth0
# NOT: default dev wg0
```

### Test 2: WireGuard Routes
```bash
# Check WireGuard routes (should only show VM subnet)
ip route show dev wg0

# Should show:
# 10.10.10.0/24 dev wg0
# NOT: default dev wg0 or 0.0.0.0/0
```

### Test 3: Host Public IP
```bash
# Check what IP the host uses
curl ifconfig.me

# Should show: 91.98.80.35 (your VPS IP)
# NOT: 141.98.102.237 (VPN IP)
```

### Test 4: Run Automated Test
```bash
sudo bash /root/vm-setup-files/test-host-connectivity.sh
```

## 📋 Safe Installation Checklist

Before running the fixed script:

1. ✓ **Backup current network config**
   ```bash
   ip route save > /root/routes-backup.txt
   iptables-save > /root/iptables-backup.txt
   ```

2. ✓ **Test SSH access**
   - Keep an existing SSH session open
   - Open a new terminal and try connecting
   - If new connection fails, you still have the open session to fix it

3. ✓ **Have console access ready**
   - Keep Hetzner web console tab open
   - You can use it if SSH breaks

4. ✓ **Run the test script AFTER installation**
   ```bash
   sudo bash test-host-connectivity.sh
   ```

## 🚨 Emergency Recovery (If Something Goes Wrong)

### Quick Fix Commands:

```bash
# 1. Stop WireGuard immediately
systemctl stop wg-airvpn
wg-quick down wg0 2>/dev/null
ip link delete wg0 2>/dev/null

# 2. Check default route is correct
ip route show default

# If default route is wrong:
ip route del default
ip route add default via <your-gateway-ip> dev <your-main-interface>

# 3. Verify connectivity
ping -c 3 8.8.8.8
curl ifconfig.me
```

## 📝 What Each Component Does Now

| Component | Purpose | Affects Host? |
|-----------|---------|---------------|
| **WireGuard (wg0)** | VPN connection for VM traffic only | ❌ No - isolated routing |
| **vm-bridge** | Private network for VM (10.10.10.0/24) | ❌ No - separate network |
| **iptables rules** | Route VM through VPN, block direct access | ❌ No - only VM traffic |
| **dnsmasq** | DHCP server for VM | ❌ No - only on vm-bridge |
| **IP forwarding** | Allow VM traffic to be forwarded | ✓ System-wide (needed) |

## 🎯 Expected Behavior After Installation

### ✅ Host (Your VPS):
- SSH works normally on port 22
- Public IP: 91.98.80.35
- Internet access through eth0 (Hetzner)
- All services accessible
- DNS resolution works
- No routing through VPN

### ✅ VM (Your Friend):
- Isolated on 10.10.10.0/24 network
- Gets IP via DHCP (10.10.10.100-200)
- ALL traffic routes through WireGuard VPN
- Public IP: 141.98.102.237 (VPN endpoint)
- Kill switch: no internet if VPN down
- Cannot access host's main network

### ✅ VPN (WireGuard):
- Connects to AirVPN Frankfurt
- Routes ONLY 10.10.10.0/24 subnet
- Does NOT route host traffic
- Auto-reconnect on failure
- Managed by systemd

## 📖 Testing the Fixed Setup

### Step-by-Step Test:

1. **Install with fixed script:**
   ```bash
   cd /root/vm-setup-files
   sudo bash install-vm.sh
   ```

2. **Immediately test host (from another terminal):**
   ```bash
   ssh root@91.98.80.35
   # Should work!
   ```

3. **Run connectivity test:**
   ```bash
   sudo bash test-host-connectivity.sh
   ```

4. **Check host public IP:**
   ```bash
   curl ifconfig.me
   # Should show: 91.98.80.35
   ```

5. **Check VM (wait 2 min for boot):**
   ```bash
   sudo virsh domifaddr isolated-vm
   ssh vmuser@10.10.10.XXX
   curl ifconfig.me
   # Should show: 141.98.102.237 (VPN IP)
   ```

6. **Test kill switch:**
   ```bash
   # In VM terminal:
   curl ifconfig.me  # Works, shows VPN IP
   
   # On host:
   sudo systemctl stop wg-airvpn
   
   # In VM terminal:
   curl ifconfig.me  # Should timeout/fail
   
   # On host:
   sudo systemctl start wg-airvpn
   
   # In VM terminal:
   curl ifconfig.me  # Works again
   ```

## 💡 Key Takeaways

1. **`Table = off` is CRITICAL** - Without it, WireGuard will hijack your entire routing table

2. **Never flush iptables blindly** - Use specific rules, don't disturb existing configuration

3. **Test incrementally** - After each major change, verify host connectivity

4. **Keep console access** - Always have Hetzner web console available as backup

5. **VM isolation is complete** - VM truly cannot access anything except through VPN

## 🆘 Still Having Issues?

Run the diagnostic:
```bash
sudo bash /root/vm-setup-files/test-host-connectivity.sh
```

Look for any red ✗ marks and follow the suggestions.

If host SSH is broken:
1. Use Hetzner web console
2. Run: `systemctl stop wg-airvpn`
3. Run: `ip route show` and verify default route
4. Fix routing if needed

---

**Bottom line:** The fixed script isolates VM traffic to VPN while keeping your host completely accessible and functional.
