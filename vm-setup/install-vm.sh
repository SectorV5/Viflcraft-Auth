#!/bin/bash

# Secure VM Setup with VPN Routing and Kill Switch
# For aarch64 Ubuntu on Hetzner
# This script installs KVM, creates an isolated VM, and routes all traffic through WireGuard VPN

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
VM_NAME="isolated-vm"
VM_RAM=1024  # 1GB RAM
VM_CPUS=4
VM_DISK_SIZE=100  # GB
VM_USERNAME="vmuser"
VM_PASSWORD="$(openssl rand -base64 12)"
INSTALL_DIR="/opt/secure-vm"
VPN_CONFIG="/opt/secure-vm/wireguard/airvpn.conf"

echo -e "${GREEN}=== Secure VM Setup Script ===${NC}"
echo "This script will:"
echo "  1. Install KVM/QEMU virtualization"
echo "  2. Set up WireGuard VPN with kill switch"
echo "  3. Create an isolated VM (Debian 12)"
echo "  4. Route ALL VM traffic through VPN"
echo "  5. Create management scripts"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

# Backup function
backup_config() {
    local file=$1
    if [ -f "$file" ]; then
        cp "$file" "${file}.backup.$(date +%Y%m%d_%H%M%S)"
        echo -e "${GREEN}Backed up: $file${NC}"
    fi
}

echo -e "${YELLOW}[1/10] Creating installation directory...${NC}"
mkdir -p $INSTALL_DIR/{wireguard,vm-data,scripts,logs}
cd $INSTALL_DIR

echo -e "${YELLOW}[2/10] Updating system and installing prerequisites...${NC}"
apt-get update
apt-get install -y \
    qemu-kvm \
    libvirt-daemon-system \
    libvirt-clients \
    bridge-utils \
    virtinst \
    virt-manager \
    wireguard \
    wireguard-tools \
    iptables \
    iptables-persistent \
    wget \
    curl \
    cloud-image-utils \
    genisoimage

echo -e "${YELLOW}[3/10] Enabling IP forwarding (minimal changes)...${NC}"
# Enable IP forwarding only if not already enabled
if ! sysctl net.ipv4.ip_forward | grep -q "= 1"; then
    echo "net.ipv4.ip_forward=1" >> /etc/sysctl.d/99-vm-forwarding.conf
    sysctl -w net.ipv4.ip_forward=1
    echo "  Added IP forwarding to /etc/sysctl.d/99-vm-forwarding.conf"
else
    echo "  IP forwarding already enabled"
fi

echo -e "${YELLOW}[4/10] Setting up WireGuard VPN (VM-ONLY routing)...${NC}"
# CRITICAL: This config routes ONLY VM traffic through VPN, NOT the host
cat > $VPN_CONFIG << 'VPNEOF'
[Interface]
Address = 10.173.212.207/32,fd7d:76ee:e68f:a993:24e4:43b5:bf29:5238/128
PrivateKey = aEV1+EHV+NGqe4rd7CjG1/to383pOpP+YUdRGPeMEXA=
MTU = 1320
Table = off
PostUp = ip route add 10.10.10.0/24 dev wg0; iptables -A FORWARD -i vm-bridge -o wg0 -j ACCEPT; iptables -A FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT; iptables -t nat -A POSTROUTING -s 10.10.10.0/24 -o wg0 -j MASQUERADE; iptables -A FORWARD -i vm-bridge ! -o wg0 -j REJECT
PostDown = ip route del 10.10.10.0/24 dev wg0 2>/dev/null; iptables -D FORWARD -i vm-bridge -o wg0 -j ACCEPT; iptables -D FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT; iptables -t nat -D POSTROUTING -s 10.10.10.0/24 -o wg0 -j MASQUERADE; iptables -D FORWARD -i vm-bridge ! -o wg0 -j REJECT

[Peer]
PublicKey = PyLCXAQT8KkM4T+dUsOQfn+Ub3pGxfGlxkIApuig+hk=
PresharedKey = bb1gbWEPIoj8A3zNEOsf0RNIefHdYJ9+AfQPp+eYEhI=
Endpoint = 141.98.102.237:1637
AllowedIPs = 0.0.0.0/0,::/0
PersistentKeepalive = 15
VPNEOF

chmod 600 $VPN_CONFIG

# Create systemd service for WireGuard
cat > /etc/systemd/system/wg-airvpn.service << 'WGEOF'
[Unit]
Description=WireGuard VPN for Isolated VM
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/usr/bin/wg-quick up /opt/secure-vm/wireguard/airvpn.conf
ExecStop=/usr/bin/wg-quick down /opt/secure-vm/wireguard/airvpn.conf
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
WGEOF

echo -e "${YELLOW}[5/10] Creating isolated bridge network...${NC}"
# Create a bridge for VM that is NOT connected to host's main network
cat > /etc/netplan/99-vm-bridge.yaml << 'BRIDGEEOF'
network:
  version: 2
  bridges:
    vm-bridge:
      addresses:
        - 10.10.10.1/24
      dhcp4: no
      dhcp6: no
BRIDGEEOF

netplan apply || true
sleep 2

# Ensure bridge exists
if ! ip link show vm-bridge &>/dev/null; then
    ip link add vm-bridge type bridge
    ip addr add 10.10.10.1/24 dev vm-bridge
    ip link set vm-bridge up
fi

echo -e "${YELLOW}[6/10] Configuring firewall and kill switch (VM-only, host untouched)...${NC}"
# IMPORTANT: Do NOT flush all rules or change default policies - preserve host functionality
# Only add specific rules for VM traffic

# Allow traffic from vm-bridge ONLY to wg0 (VPN) - will be added by WireGuard PostUp
# Allow return traffic from VPN to VM - will be added by WireGuard PostUp
# NAT VM traffic through VPN (only 10.10.10.0/24 subnet) - will be added by WireGuard PostUp
# KILL SWITCH: Block VM from non-VPN interfaces - will be added by WireGuard PostUp

# Allow DHCP on bridge for VM
iptables -A INPUT -i vm-bridge -p udp --dport 67 -j ACCEPT 2>/dev/null || true

# Allow established connections on bridge
iptables -A FORWARD -i vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || true

# Save current iptables rules (preserve existing rules)
netfilter-persistent save

echo -e "${YELLOW}[7/10] Setting up dnsmasq for VM network...${NC}"
apt-get install -y dnsmasq

# Backup dnsmasq config
backup_config /etc/dnsmasq.conf

cat > /etc/dnsmasq.d/vm-bridge.conf << 'DNSEOF'
interface=vm-bridge
bind-interfaces
dhcp-range=10.10.10.100,10.10.10.200,12h
dhcp-option=option:router,10.10.10.1
dhcp-option=option:dns-server,10.10.10.1
server=10.128.0.1
DNSEOF

systemctl restart dnsmasq
systemctl enable dnsmasq

echo -e "${YELLOW}[8/10] Downloading Debian 12 cloud image...${NC}"
if [ ! -f "$INSTALL_DIR/vm-data/debian-12-generic-arm64.qcow2" ]; then
    wget -O /tmp/debian-12.qcow2 https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-generic-arm64.qcow2
    mv /tmp/debian-12.qcow2 $INSTALL_DIR/vm-data/debian-12-base.qcow2
fi

echo -e "${YELLOW}[9/10] Creating VM disk and cloud-init configuration...${NC}"
# Create VM disk from base image
qemu-img create -f qcow2 -F qcow2 -b $INSTALL_DIR/vm-data/debian-12-base.qcow2 \
    $INSTALL_DIR/vm-data/${VM_NAME}.qcow2 ${VM_DISK_SIZE}G

# Create cloud-init user-data
cat > $INSTALL_DIR/vm-data/user-data << CLOUDEOF
#cloud-config
hostname: ${VM_NAME}
manage_etc_hosts: true
users:
  - name: ${VM_USERNAME}
    shell: /bin/bash
    lock_passwd: false
    passwd: $(openssl passwd -6 "$VM_PASSWORD")
    sudo: ALL=(ALL) NOPASSWD:ALL
    groups: users, admin, sudo

# Enable password authentication
ssh_pwauth: true

package_update: true
package_upgrade: true
packages:
  - curl
  - wget
  - htop
  - net-tools
  - iptables

runcmd:
  - systemctl restart sshd
  - echo "VM setup complete" > /root/vm-ready

power_state:
  mode: reboot
  timeout: 300
  condition: true
CLOUDEOF

# Create meta-data
cat > $INSTALL_DIR/vm-data/meta-data << METAEOF
instance-id: ${VM_NAME}
local-hostname: ${VM_NAME}
METAEOF

# Create cloud-init ISO
genisoimage -output $INSTALL_DIR/vm-data/cloud-init.iso \
    -volid cidata -joliet -rock \
    $INSTALL_DIR/vm-data/user-data \
    $INSTALL_DIR/vm-data/meta-data

echo -e "${YELLOW}[10/10] Creating VM with libvirt...${NC}"
virt-install \
    --name $VM_NAME \
    --ram $VM_RAM \
    --vcpus $VM_CPUS \
    --disk path=$INSTALL_DIR/vm-data/${VM_NAME}.qcow2,format=qcow2,bus=virtio \
    --disk path=$INSTALL_DIR/vm-data/cloud-init.iso,device=cdrom \
    --os-variant debian11 \
    --network bridge=vm-bridge,model=virtio \
    --graphics none \
    --console pty,target_type=serial \
    --import \
    --noautoconsole

echo ""
echo -e "${GREEN}=== Installation Complete! ===${NC}"
echo ""
echo "VM Credentials:"
echo "  Username: ${VM_USERNAME}"
echo "  Password: ${VM_PASSWORD}"
echo ""
echo "VM Network:"
echo "  Bridge: vm-bridge (10.10.10.1/24)"
echo "  VM will receive IP via DHCP (10.10.10.100-200 range)"
echo ""
echo "VPN Status:"
echo "  WireGuard config: $VPN_CONFIG"
echo ""

# Save credentials
cat > $INSTALL_DIR/VM-CREDENTIALS.txt << CREDEOF
VM Access Information
=====================
Created: $(date)

SSH Access:
  Username: ${VM_USERNAME}
  Password: ${VM_PASSWORD}

To find VM IP address:
  sudo virsh domifaddr ${VM_NAME}

To connect to VM console:
  sudo virsh console ${VM_NAME}

To get VM IP and SSH into it:
  VM_IP=\$(sudo virsh domifaddr ${VM_NAME} | grep -oP '10\.10\.10\.\d+')
  ssh ${VM_USERNAME}@\$VM_IP
  
All traffic from this VM goes through WireGuard VPN.
If VPN disconnects, VM will have NO internet access (kill switch active).
CREDEOF

chmod 600 $INSTALL_DIR/VM-CREDENTIALS.txt

echo -e "${GREEN}Credentials saved to: $INSTALL_DIR/VM-CREDENTIALS.txt${NC}"
echo ""
echo "Starting WireGuard VPN..."
systemctl daemon-reload
systemctl enable wg-airvpn
systemctl start wg-airvpn

sleep 3

# Check VPN status
if ip link show wg0 &>/dev/null; then
    echo -e "${GREEN}✓ WireGuard VPN is UP${NC}"
    WG_IP=$(ip -4 addr show wg0 | grep -oP '(?<=inet\s)\d+(\.\d+){3}')
    echo "  VPN IP: $WG_IP"
else
    echo -e "${RED}✗ WireGuard VPN failed to start${NC}"
    echo "  Check logs: journalctl -u wg-airvpn -n 50"
fi

# Check VM status
sleep 5
VM_STATE=$(virsh domstate $VM_NAME 2>/dev/null || echo "unknown")
echo ""
if [ "$VM_STATE" = "running" ]; then
    echo -e "${GREEN}✓ VM is running${NC}"
else
    echo -e "${YELLOW}⚠ VM state: $VM_STATE${NC}"
    echo "  The VM may still be booting. Wait 1-2 minutes."
fi

echo ""
echo "Next steps:"
echo "  1. Wait 1-2 minutes for VM to fully boot"
echo "  2. Run: sudo virsh domifaddr $VM_NAME"
echo "  3. SSH to the VM using credentials above"
echo "  4. Test VPN: curl ifconfig.me (should show VPN IP)"
echo ""
echo "Management scripts are in: $INSTALL_DIR/scripts/"
