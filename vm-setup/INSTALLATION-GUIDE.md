# 🚀 Step-by-Step Installation Guide

## Quick Start (Already Uploaded!)

Good news! All files have been uploaded to your VPS at `/root/vm-setup-files/`

### Installation Steps

**1. SSH into your VPS:**

```bash
ssh root@91.98.80.35
```

**2. Navigate to the setup directory:**

```bash
cd /root/vm-setup-files
```

**3. Verify files are present:**

```bash
ls -lh
```

You should see:
- `install-vm.sh` - Main installation script
- `vm-start.sh` - Start VM script
- `vm-stop.sh` - Stop VM script  
- `vm-delete.sh` - Delete VM script
- `vpn-change.sh` - Change VPN config script
- `vpn-status.sh` - Status checker script
- `uninstall.sh` - Complete removal script
- `README.md` - Full documentation

**4. Run the installation:**

```bash
sudo bash install-vm.sh
```

**Installation will:**
- Install KVM, QEMU, libvirt (virtualization)
- Install WireGuard VPN
- Download Debian 12 ARM64 cloud image (~500MB)
- Create isolated network bridge
- Configure firewall kill switch
- Set up VM with 1GB RAM, 4 CPUs, 100GB disk
- Configure VPN routing

**Time:** 5-15 minutes (depends on download speed)

**5. Installation complete! Get VM credentials:**

```bash
sudo cat /opt/secure-vm/VM-CREDENTIALS.txt
```

You'll see:
```
Username: vmuser
Password: <randomly-generated-password>
```

**6. Find VM IP address:**

Wait 1-2 minutes for VM to boot, then:

```bash
sudo virsh domifaddr isolated-vm
```

Output example:
```
Name       MAC address          Protocol     Address
-------------------------------------------------------------------------------
vnet0      52:54:00:xx:xx:xx    ipv4         10.10.10.123/24
```

**7. SSH into the VM:**

```bash
ssh vmuser@10.10.10.123
# Use password from step 5
```

**8. Test VPN connection (inside VM):**

```bash
# Check your public IP (should be VPN IP, not Hetzner IP)
curl ifconfig.me

# Should show: 141.98.102.237 (or similar VPN exit IP)
# NOT: 91.98.80.35 (Hetzner IP)
```

---

## ✅ Verify Everything Works

### Test 1: VPN Status

```bash
# On VPS host
sudo systemctl status wg-airvpn
sudo wg show
```

Should show: Active and connected

### Test 2: VM Network

```bash
# On VPS host
sudo virsh domifaddr isolated-vm
```

Should show: 10.10.10.xxx IP address

### Test 3: Kill Switch

```bash
# Terminal 1: SSH to VM
ssh vmuser@10.10.10.xxx
curl ifconfig.me  # Note the IP (VPN IP)

# Terminal 2: On VPS host, stop VPN
sudo systemctl stop wg-airvpn

# Terminal 1: In VM, try curl again
curl ifconfig.me  # Should FAIL or timeout (kill switch working!)

# Terminal 2: Restart VPN
sudo systemctl start wg-airvpn

# Terminal 1: Try again
curl ifconfig.me  # Should work again with VPN IP
```

---

## 📋 Management Scripts

All scripts are in `/opt/secure-vm/scripts/` after installation:

### Start VM
```bash
sudo /opt/secure-vm/scripts/vm-start.sh
```

### Stop VM
```bash
sudo /opt/secure-vm/scripts/vm-stop.sh
```

### Check Status
```bash
sudo /opt/secure-vm/scripts/vpn-status.sh
```

### Change VPN Config
```bash
sudo /opt/secure-vm/scripts/vpn-change.sh
```

### Delete VM (keeps infrastructure)
```bash
sudo /opt/secure-vm/scripts/vm-delete.sh
```

### Complete Uninstall (removes everything)
```bash
sudo /opt/secure-vm/scripts/uninstall.sh
```

---

## 🔧 Troubleshooting

### Problem: VM won't start

```bash
sudo systemctl status libvirtd
sudo systemctl start libvirtd
sudo virsh list --all
```

### Problem: No internet in VM

```bash
# Check VPN
sudo systemctl status wg-airvpn
sudo wg show

# Restart VPN
sudo systemctl restart wg-airvpn
```

### Problem: Can't find VM IP

```bash
# Wait 60 seconds, then:
sudo virsh domifaddr isolated-vm

# Alternative: Check DHCP leases
sudo cat /var/lib/misc/dnsmasq.leases
```

### Problem: VPN won't connect

```bash
# Check logs
sudo journalctl -u wg-airvpn -n 50

# Test manually
sudo wg-quick up /opt/secure-vm/wireguard/airvpn.conf
```

---

## 📱 Giving Friend Access

### Option 1: Direct Access (needs 2 SSH hops)

Your friend connects:
```bash
# Step 1: SSH to VPS
ssh root@91.98.80.35

# Step 2: Get VM IP
sudo virsh domifaddr isolated-vm

# Step 3: SSH to VM
ssh vmuser@10.10.10.xxx
```

### Option 2: SSH Jump Host (easier)

Your friend can use:
```bash
ssh -J root@91.98.80.35 vmuser@10.10.10.xxx
```

### Option 3: Create dedicated user on VPS (most secure)

On VPS:
```bash
# Create user for friend
sudo adduser friend
sudo usermod -aG libvirt friend

# Give them VM IP
sudo virsh domifaddr isolated-vm
```

Send your friend:
- VPS IP: 91.98.80.35
- VPS username: friend
- VPS password: (what you set)
- VM IP: 10.10.10.xxx
- VM username: vmuser
- VM password: (from /opt/secure-vm/VM-CREDENTIALS.txt)

---

## 🗑️ If You Change Your Mind

Complete removal:
```bash
sudo /opt/secure-vm/scripts/uninstall.sh
```

Type `DELETE EVERYTHING` when prompted.

This removes:
- VM and all data
- VPN configuration
- Network bridge
- Firewall rules
- All scripts

Your VPS returns to original state.

---

## 📁 File Locations Reference

```
/opt/secure-vm/
├── wireguard/
│   └── airvpn.conf              # VPN config
├── vm-data/
│   ├── debian-12-base.qcow2     # Base image
│   └── isolated-vm.qcow2        # VM disk (100GB)
├── scripts/
│   ├── vm-start.sh
│   ├── vm-stop.sh
│   ├── vm-delete.sh
│   ├── vpn-change.sh
│   ├── vpn-status.sh
│   └── uninstall.sh
└── VM-CREDENTIALS.txt           # LOGIN INFO HERE!

/etc/systemd/system/
└── wg-airvpn.service            # VPN service

/etc/netplan/
└── 99-vm-bridge.yaml            # Bridge config

/etc/dnsmasq.d/
└── vm-bridge.conf               # DHCP config
```

---

## 🎓 What You Can Do in the VM

Your friend can:
- Install and run web servers (Nginx, Apache)
- Host websites and applications
- Run databases (MySQL, PostgreSQL, MongoDB)
- Use Docker containers
- Install any Linux software
- Run game servers
- Set up development environments

**All traffic goes through VPN!**
- Real IP (91.98.80.35) is hidden
- VPN IP (141.98.102.237) is exposed
- If VPN drops, VM goes offline (no leaks)

---

## 💡 Pro Tips

1. **Auto-start VM on boot:**
   ```bash
   sudo virsh autostart isolated-vm
   ```

2. **Monitor VM resource usage:**
   ```bash
   sudo virsh domstats isolated-vm
   ```

3. **Backup VM disk:**
   ```bash
   sudo cp /opt/secure-vm/vm-data/isolated-vm.qcow2 \
      /backup/isolated-vm-$(date +%Y%m%d).qcow2
   ```

4. **Increase VM resources later:**
   ```bash
   # Stop VM first
   sudo virsh shutdown isolated-vm
   
   # Edit config
   sudo virsh edit isolated-vm
   
   # Change <memory> and <vcpu> values
   ```

5. **View VM console (if SSH fails):**
   ```bash
   sudo virsh console isolated-vm
   # Press Ctrl+] to exit
   ```

---

## 📞 Need Help?

Run the status checker:
```bash
sudo /opt/secure-vm/scripts/vpn-status.sh
```

Check all logs:
```bash
# VPN logs
sudo journalctl -u wg-airvpn -n 100

# VM logs
sudo virsh console isolated-vm

# System logs
sudo journalctl -xe
```

---

## ✨ Summary

**What you have:**
- Isolated VM (1GB RAM, 4 CPUs, 100GB disk)
- Debian 12 operating system
- All traffic through WireGuard VPN
- Kill switch prevents IP leaks
- Easy management scripts

**Location:** `/opt/secure-vm/`

**Credentials:** `/opt/secure-vm/VM-CREDENTIALS.txt`

**Start VM:** `sudo /opt/secure-vm/scripts/vm-start.sh`

**Status:** `sudo /opt/secure-vm/scripts/vpn-status.sh`

**Uninstall:** `sudo /opt/secure-vm/scripts/uninstall.sh`

---

*Ready to start? Just run:* `sudo bash /root/vm-setup-files/install-vm.sh`
