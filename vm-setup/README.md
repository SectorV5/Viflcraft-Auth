# Secure Isolated VM with VPN Routing and Kill Switch

This setup creates a fully isolated virtual machine on your Ubuntu VPS that routes **ALL** traffic through a WireGuard VPN connection. If the VPN disconnects, the VM loses internet access completely (kill switch), preventing IP leaks.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Installation](#installation)
- [Usage](#usage)
- [Management Scripts](#management-scripts)
- [Security Features](#security-features)
- [Troubleshooting](#troubleshooting)
- [Uninstallation](#uninstallation)
- [Technical Details](#technical-details)

---

## 🎯 Overview

**What This Does:**
- Creates a KVM/QEMU virtual machine (Debian 12) with 1GB RAM, 4 CPU cores, 100GB disk
- Isolates the VM on a private network bridge (no direct host network access)
- Routes ALL VM traffic through WireGuard VPN
- Implements a kill switch: VM has ZERO internet if VPN disconnects
- Provides SSH access with username/password authentication
- Includes management scripts for easy control

**What This Protects Against:**
- IP leaks from the VM to your VPS provider (Hetzner)
- Accidental VPN disconnections exposing real IP
- VM bypassing VPN through host network

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Hetzner VPS (Ubuntu)                  │
│  ┌────────────┐          ┌──────────────┐              │
│  │   Host     │          │  WireGuard   │              │
│  │   OS       │◄────────►│  VPN (wg0)   │◄────► Internet (VPN)
│  │            │          │  10.173.x.x  │              │
│  └────────────┘          └──────────────┘              │
│        │                                                │
│        │ ISOLATED                                       │
│        ▼                                                │
│  ┌────────────┐          ┌──────────────┐              │
│  │ vm-bridge  │          │  iptables    │              │
│  │ 10.10.10.1 │◄────────►│  KILL SWITCH │              │
│  └────────────┘          │  FIREWALL    │              │
│        │                 └──────────────┘              │
│        │ ONLY TO VPN                                    │
│        ▼                                                │
│  ┌────────────┐                                         │
│  │  Guest VM  │  ✗ No direct access to eth0            │
│  │  Debian 12 │  ✓ All traffic through wg0 only        │
│  │ 10.10.10.x │  ✗ Internet blocked if VPN down        │
│  └────────────┘                                         │
└─────────────────────────────────────────────────────────┘
```

**Network Flow:**
1. VM sends traffic → vm-bridge (10.10.10.1)
2. Host firewall checks: Is VPN up?
   - YES: Route to wg0 (VPN) → Internet
   - NO: **REJECT** (kill switch activated)
3. Return traffic: Internet → wg0 → vm-bridge → VM

---

## 📦 Installation

### Prerequisites

- Ubuntu 22.04+ on aarch64 (ARM64) architecture
- Root access
- At least 4GB RAM free (host needs 3GB, VM gets 1GB)
- 100GB+ free disk space
- WireGuard VPN configuration file

### Step 1: Upload Files to VPS

Upload these files to your VPS:
- `install-vm.sh` - Main installation script
- `AirVPN.conf` - Your WireGuard VPN configuration

```bash
# From your local machine:
scp -i id_rsa install-vm.sh root@91.98.80.35:/root/
scp -i id_rsa AirVPN.conf root@91.98.80.35:/root/
scp -i id_rsa vm-*.sh vpn-*.sh uninstall.sh root@91.98.80.35:/root/
```

### Step 2: SSH into VPS

```bash
ssh -i id_rsa root@91.98.80.35
```

### Step 3: Run Installation

```bash
cd /root
chmod +x install-vm.sh
sudo ./install-vm.sh
```

**Installation takes 5-15 minutes** depending on internet speed (downloads Debian 12 image).

### Step 4: Wait for VM Boot

After installation completes, wait 1-2 minutes for the VM to fully boot.

### Step 5: Get VM Credentials

```bash
sudo cat /opt/secure-vm/VM-CREDENTIALS.txt
```

You'll see:
```
Username: vmuser
Password: <generated-password>
```

### Step 6: Find VM IP and Connect

```bash
# Get VM IP address
sudo virsh domifaddr isolated-vm

# SSH into VM
ssh vmuser@10.10.10.XXX
```

---

## 🚀 Usage

### Accessing the VM

1. **From the VPS host:**
   ```bash
   # Get VM IP
   sudo virsh domifaddr isolated-vm
   
   # SSH to VM
   ssh vmuser@10.10.10.XXX
   ```

2. **From outside (for your friend):**
   
   Your friend needs to SSH to the VPS first, then to the VM:
   ```bash
   # Step 1: SSH to VPS
   ssh root@91.98.80.35
   
   # Step 2: SSH to VM
   ssh vmuser@10.10.10.XXX
   ```

   **Alternative: Port Forwarding** (more convenient)
   
   Add to VPS `/etc/ssh/sshd_config`:
   ```
   AllowTcpForwarding yes
   GatewayPorts yes
   ```
   
   Then restart SSH:
   ```bash
   systemctl restart sshd
   ```
   
   Now your friend can SSH directly:
   ```bash
   ssh -J root@91.98.80.35 vmuser@10.10.10.XXX
   ```

### Testing VPN Connection

Once inside the VM:

```bash
# Check your public IP (should be VPN IP, not Hetzner IP)
curl ifconfig.me

# Check DNS
dig +short myip.opendns.com @resolver1.opendns.com

# Verify location
curl ipinfo.io
```

### Testing Kill Switch

1. SSH into VM
2. In another terminal on the VPS, stop the VPN:
   ```bash
   sudo systemctl stop wg-airvpn
   ```
3. In the VM, try to access internet:
   ```bash
   curl ifconfig.me  # Should fail/timeout
   ping 8.8.8.8      # Should fail
   ```
4. Restart VPN on VPS:
   ```bash
   sudo systemctl start wg-airvpn
   ```
5. Internet should work again in VM

---

## 🛠️ Management Scripts

All scripts are located in `/opt/secure-vm/scripts/` after installation:

### 1. **Start VM** (`vm-start.sh`)

Starts the VM and ensures VPN is running.

```bash
sudo /opt/secure-vm/scripts/vm-start.sh
```

**What it does:**
- Checks if WireGuard VPN is running
- Starts VPN if not running
- Starts the VM
- Displays status information

---

### 2. **Stop VM** (`vm-stop.sh`)

Gracefully shuts down the VM.

```bash
sudo /opt/secure-vm/scripts/vm-stop.sh
```

**What it does:**
- Attempts graceful shutdown (30 seconds timeout)
- Force stops if VM doesn't respond
- Keeps VPN running (for quick restart)

---

### 3. **Delete VM** (`vm-delete.sh`)

Permanently deletes the VM and all its data.

```bash
sudo /opt/secure-vm/scripts/vm-delete.sh
```

**What it does:**
- Prompts for confirmation
- Stops and undefines VM
- Removes all VM disk files
- Keeps infrastructure (VPN, bridge, scripts)

**Use case:** When you want to create a fresh VM

---

### 4. **Change VPN** (`vpn-change.sh`)

Change or update VPN configuration.

```bash
sudo /opt/secure-vm/scripts/vpn-change.sh
```

**Options:**
1. Replace with new WireGuard config file
2. Edit current config manually
3. View current config (hides private keys)
4. Cancel

**Automatic features:**
- Backs up old config before changes
- Adds kill switch rules if missing
- Restarts VPN with new config
- Reverts to backup if new config fails

---

### 5. **VPN Status** (`vpn-status.sh`)

Check status of VPN, VM, and connectivity.

```bash
sudo /opt/secure-vm/scripts/vpn-status.sh
```

**Shows:**
- WireGuard VPN status and IP
- VM running state
- VM IP address
- Connectivity tests
- Firewall rules
- Bridge network info

---

### 6. **Uninstall Everything** (`uninstall.sh`)

Completely removes all traces of the setup.

```bash
sudo /opt/secure-vm/scripts/uninstall.sh
```

**Removes:**
- VM and all data
- WireGuard VPN config
- Network bridge
- Firewall rules
- All installation files
- Optionally: installed packages

**⚠️ WARNING:** This cannot be undone! Make backups first.

---

## 🔒 Security Features

### 1. **Network Isolation**

The VM is on a completely isolated bridge network (`vm-bridge`) with no direct access to the host's main network interface.

**Verification:**
```bash
# On VPS host
ip route show dev vm-bridge
# Should only show 10.10.10.0/24 network
```

### 2. **Kill Switch (IP Leak Protection)**

Firewall rules ensure VM traffic can ONLY exit through the VPN:

```bash
# Allow VM → VPN
iptables -A FORWARD -i vm-bridge -o wg0 -j ACCEPT

# Allow VPN → VM (established connections)
iptables -A FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT

# KILL SWITCH: Reject VM traffic to any other interface
iptables -A FORWARD -i vm-bridge ! -o wg0 -j REJECT
```

**What this means:**
- If VPN (`wg0`) is down, the last rule blocks ALL VM internet access
- VM cannot accidentally route through host's main interface
- No IP leaks possible

### 3. **Automatic VPN Monitoring**

WireGuard is configured as a systemd service with:
- Automatic restart on failure
- PersistentKeepalive to maintain connection
- PostUp/PostDown hooks to manage firewall rules

### 4. **DNS Isolation**

VM uses DNS servers from the VPN provider (not host's DNS):
```
DNS = 10.128.0.1
```

This prevents DNS leaks through the host.

---

## 🔧 Troubleshooting

### VM Won't Start

**Check libvirt service:**
```bash
sudo systemctl status libvirtd
sudo systemctl start libvirtd
```

**Check VM definition:**
```bash
sudo virsh list --all
```

**View VM console:**
```bash
sudo virsh console isolated-vm
# Press Ctrl+] to exit console
```

---

### VM Has No Internet

**Check VPN status:**
```bash
sudo systemctl status wg-airvpn
sudo wg show
```

**Restart VPN:**
```bash
sudo systemctl restart wg-airvpn
```

**Check firewall rules:**
```bash
sudo iptables -L FORWARD -n -v | grep vm-bridge
```

**Check if VM can reach gateway:**
```bash
# From inside VM
ping 10.10.10.1  # Should work (host bridge)
ping 8.8.8.8     # Should work if VPN is up
```

---

### Can't Find VM IP

**Wait 60 seconds after VM start**, then:
```bash
sudo virsh domifaddr isolated-vm
```

If still no IP:
```bash
# Check DHCP leases
sudo cat /var/lib/misc/dnsmasq.leases

# Check VM network interface
sudo virsh domiflist isolated-vm
```

---

### VPN Won't Start

**Check WireGuard config syntax:**
```bash
sudo wg-quick up /opt/secure-vm/wireguard/airvpn.conf
# Look for error messages
```

**Check logs:**
```bash
sudo journalctl -u wg-airvpn -n 50
```

**Common issues:**
- Incorrect endpoint IP/port
- Firewall blocking WireGuard port (1637 UDP)
- Invalid keys in config

**Test manually:**
```bash
sudo wg-quick up /opt/secure-vm/wireguard/airvpn.conf
sudo wg show
sudo wg-quick down /opt/secure-vm/wireguard/airvpn.conf
```

---

### Check if Kill Switch Works

```bash
# Terminal 1: SSH to VM
ssh vmuser@10.10.10.XXX

# Terminal 2: On VPS, stop VPN
sudo systemctl stop wg-airvpn

# Terminal 1: In VM, test internet
curl ifconfig.me  # Should fail/hang
ping 8.8.8.8      # Should fail

# Terminal 2: Restart VPN
sudo systemctl start wg-airvpn

# Terminal 1: Test again
curl ifconfig.me  # Should work and show VPN IP
```

---

## 🗑️ Uninstallation

### Complete Removal

```bash
cd /opt/secure-vm/scripts
sudo ./uninstall.sh
```

Type `DELETE EVERYTHING` when prompted.

**This removes:**
- ✓ VM and all data
- ✓ WireGuard VPN
- ✓ Network bridge
- ✓ Firewall rules  
- ✓ All scripts and configs
- ✓ /opt/secure-vm directory

**Optionally removes:**
- Installed packages (qemu-kvm, libvirt, wireguard, etc.)

---

### Partial Removal (Keep Infrastructure)

To remove just the VM but keep VPN and scripts:

```bash
sudo /opt/secure-vm/scripts/vm-delete.sh
```

---

### Manual Rollback (If Script Fails)

```bash
# Stop services
sudo systemctl stop wg-airvpn
sudo virsh destroy isolated-vm

# Remove VM
sudo virsh undefine isolated-vm --remove-all-storage

# Remove WireGuard
sudo wg-quick down /opt/secure-vm/wireguard/airvpn.conf
sudo ip link delete wg0

# Remove bridge
sudo ip link set vm-bridge down
sudo ip link delete vm-bridge
sudo rm -f /etc/netplan/99-vm-bridge.yaml
sudo netplan apply

# Remove firewall rules
sudo iptables -F FORWARD
sudo iptables -P FORWARD ACCEPT
sudo iptables -t nat -F POSTROUTING
sudo netfilter-persistent save

# Remove files
sudo rm -rf /opt/secure-vm
sudo rm -f /etc/systemd/system/wg-airvpn.service
sudo systemctl daemon-reload
```

---

## 📚 Technical Details

### Installed Packages

```
qemu-kvm                 # KVM virtualization
libvirt-daemon-system    # Libvirt daemon
libvirt-clients          # Libvirt CLI tools
bridge-utils             # Network bridge utilities
virtinst                 # VM installation tools
virt-manager             # VM management
wireguard                # WireGuard VPN
wireguard-tools          # WireGuard utilities
iptables                 # Firewall
iptables-persistent      # Save firewall rules
dnsmasq                  # DHCP/DNS for VM network
cloud-image-utils        # Cloud image tools
genisoimage              # ISO creation
```

### File Locations

```
/opt/secure-vm/
├── wireguard/
│   └── airvpn.conf              # WireGuard VPN config
├── vm-data/
│   ├── debian-12-base.qcow2     # Base Debian image
│   ├── isolated-vm.qcow2        # VM disk
│   ├── cloud-init.iso           # Cloud-init config
│   ├── user-data                # Cloud-init user data
│   └── meta-data                # Cloud-init metadata
├── scripts/
│   ├── vm-start.sh              # Start VM
│   ├── vm-stop.sh               # Stop VM
│   ├── vm-delete.sh             # Delete VM
│   ├── vpn-change.sh            # Change VPN config
│   ├── vpn-status.sh            # Check status
│   └── uninstall.sh             # Complete removal
├── logs/                        # Log directory (for future use)
└── VM-CREDENTIALS.txt           # VM login credentials

/etc/systemd/system/
└── wg-airvpn.service            # WireGuard systemd service

/etc/netplan/
└── 99-vm-bridge.yaml            # Bridge network config

/etc/dnsmasq.d/
└── vm-bridge.conf               # DHCP config for VM network
```

### Network Configuration

**Host Networks:**
- `eth0`: Main VPS interface (connected to Hetzner)
- `wg0`: WireGuard VPN interface (10.173.212.207/32)
- `vm-bridge`: Isolated bridge for VM (10.10.10.1/24)

**VM Network:**
- Interface: `enp1s0` (inside VM)
- DHCP range: 10.10.10.100 - 10.10.10.200
- Gateway: 10.10.10.1 (host bridge)
- DNS: 10.128.0.1 (VPN provider)

### Firewall Rules (Detailed)

```bash
# Allow VM traffic to VPN
iptables -A FORWARD -i vm-bridge -o wg0 -j ACCEPT

# Allow return traffic from VPN to VM
iptables -A FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT

# NAT VM traffic through VPN
iptables -t nat -A POSTROUTING -o wg0 -j MASQUERADE

# KILL SWITCH: Block VM from accessing any non-VPN interface
iptables -A FORWARD -i vm-bridge ! -o wg0 -j REJECT --reject-with icmp-host-unreachable

# Allow DHCP on bridge (so VM can get IP)
iptables -A INPUT -i vm-bridge -p udp --dport 67 -j ACCEPT

# Default FORWARD policy: DROP (explicit whitelist approach)
iptables -P FORWARD DROP
```

### VM Specifications

- **CPU:** 4 cores (host CPU passthrough)
- **RAM:** 1GB (1024MB)
- **Disk:** 100GB (qcow2, thin provisioned)
- **OS:** Debian 12 (Bookworm) ARM64
- **Network:** Single virtio interface on vm-bridge
- **Boot:** Cloud-init based (automated configuration)

### Cloud-Init Configuration

The VM is provisioned using cloud-init with:
- Hostname: isolated-vm
- User: vmuser (sudo access)
- Password: randomly generated (12 chars, base64)
- SSH: password authentication enabled
- Packages: curl, wget, htop, net-tools, iptables

---

## 🎓 Usage Scenarios

### For Web Hosting

Your friend can install and run:
- Nginx/Apache web servers
- Node.js/Python applications
- Databases (MySQL, PostgreSQL, MongoDB)
- Docker containers

**All served through the VPN IP**, hiding the Hetzner IP.

### For Development

Use as isolated development environment:
- Test applications
- Run builds
- Experiments without affecting host

### For Privacy-Sensitive Tasks

- Web scraping
- Research
- Testing geolocation-based services

---

## 🆘 Getting Help

### View All Logs

```bash
# WireGuard VPN logs
sudo journalctl -u wg-airvpn -f

# VM console output
sudo virsh console isolated-vm

# libvirt logs
sudo journalctl -u libvirtd -f

# DHCP logs
sudo journalctl -u dnsmasq -f

# Firewall logs (if enabled)
sudo tail -f /var/log/kern.log | grep -i "iptables"
```

### Quick Status Check

```bash
sudo /opt/secure-vm/scripts/vpn-status.sh
```

### Common Commands

```bash
# List all VMs
sudo virsh list --all

# VM info
sudo virsh dominfo isolated-vm

# VM network
sudo virsh domifaddr isolated-vm

# Start VM
sudo virsh start isolated-vm

# Stop VM (graceful)
sudo virsh shutdown isolated-vm

# Stop VM (force)
sudo virsh destroy isolated-vm

# Connect to VM console
sudo virsh console isolated-vm
# Exit with: Ctrl + ]

# WireGuard status
sudo wg show

# Bridge status
ip addr show vm-bridge
```

---

## 📝 Notes

### Windows VM Option

While this setup uses Debian 12, you can install Windows:

1. Download Windows Server ARM64 ISO (requires license)
2. Modify `install-vm.sh` to use ISO instead of cloud image
3. Increase RAM to at least 2GB
4. Manual configuration needed (no cloud-init)

**Note:** Windows for ARM64 has limited availability. Debian/Ubuntu recommended.

### Performance Considerations

- **VPN overhead:** Expect 10-20% speed reduction due to encryption
- **Nested virtualization:** Not applicable (single VM layer)
- **Disk I/O:** qcow2 format has slight overhead vs raw, but saves space

### Security Hardening (Optional)

For production use, consider:

1. **Change SSH port** (VM and host)
2. **Disable password auth**, use SSH keys only
3. **Install fail2ban** for brute force protection
4. **Enable UFW firewall** inside VM
5. **Regular updates:**
   ```bash
   # Inside VM
   sudo apt update && sudo apt upgrade -y
   ```

---

## 📞 Support

If you encounter issues not covered in this README:

1. Check `/opt/secure-vm/logs/` for error logs
2. Run diagnostics: `sudo /opt/secure-vm/scripts/vpn-status.sh`
3. Review system logs: `sudo journalctl -xe`

---

## 📄 License

This setup is provided as-is for personal use. Modify and distribute freely.

---

## ✅ Quick Reference

### Start Everything
```bash
sudo systemctl start wg-airvpn
sudo /opt/secure-vm/scripts/vm-start.sh
```

### Stop Everything
```bash
sudo /opt/secure-vm/scripts/vm-stop.sh
sudo systemctl stop wg-airvpn
```

### Check Status
```bash
sudo /opt/secure-vm/scripts/vpn-status.sh
```

### Access VM
```bash
# Get IP
sudo virsh domifaddr isolated-vm

# SSH
ssh vmuser@10.10.10.XXX

# Credentials
sudo cat /opt/secure-vm/VM-CREDENTIALS.txt
```

### Test Kill Switch
```bash
# Stop VPN
sudo systemctl stop wg-airvpn

# In VM, try internet (should fail)
curl ifconfig.me

# Restart VPN
sudo systemctl start wg-airvpn

# In VM, try internet (should work)
curl ifconfig.me
```

---

**Installation Date:** `$(date)`  
**VPS IP:** 91.98.80.35  
**VPN Location:** Frankfurt, Germany  
**System:** Ubuntu aarch64 (Hetzner Ampere)

---

*This README was generated as part of the Secure VM setup process.*
