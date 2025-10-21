#!/bin/bash

# Complete uninstallation script - removes everything

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

echo -e "${RED}=== Complete Uninstallation ===${NC}"
echo ""
echo "This will remove:"
echo "  - VM and all its data"
echo "  - WireGuard VPN configuration"
echo "  - Network bridge"
echo "  - Firewall rules"
echo "  - All installed packages"
echo "  - /opt/secure-vm directory"
echo ""
echo -e "${RED}WARNING: This cannot be undone!${NC}"
echo ""
echo -n "Are you sure? (type 'DELETE EVERYTHING' to confirm): "
read CONFIRM

if [ "$CONFIRM" != "DELETE EVERYTHING" ]; then
    echo "Aborted."
    exit 0
fi

echo ""
echo -e "${YELLOW}[1/9] Stopping and removing VM...${NC}"
virsh destroy isolated-vm 2>/dev/null || true
virsh undefine isolated-vm --remove-all-storage 2>/dev/null || true

echo -e "${YELLOW}[2/9] Stopping WireGuard VPN...${NC}"
systemctl stop wg-airvpn 2>/dev/null || true
systemctl disable wg-airvpn 2>/dev/null || true
rm -f /etc/systemd/system/wg-airvpn.service

echo -e "${YELLOW}[3/9] Bringing down WireGuard interface...${NC}"
wg-quick down /opt/secure-vm/wireguard/airvpn.conf 2>/dev/null || true
ip link delete wg0 2>/dev/null || true

echo -e "${YELLOW}[4/9] Removing firewall rules...${NC}"
# Remove VM-specific firewall rules
iptables -D FORWARD -i vm-bridge -o wg0 -j ACCEPT 2>/dev/null || true
iptables -D FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || true
iptables -t nat -D POSTROUTING -s 10.10.10.0/24 -o wg0 -j MASQUERADE 2>/dev/null || true
iptables -D FORWARD -i vm-bridge ! -o wg0 -j REJECT 2>/dev/null || true
iptables -D INPUT -i vm-bridge -p udp --dport 67 -j ACCEPT 2>/dev/null || true
iptables -D FORWARD -i vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || true

# Do NOT change default policies - leave host firewall as it was
# Save current rules (preserving host configuration)
netfilter-persistent save

echo -e "${YELLOW}[5/9] Removing network bridge...${NC}"
ip link set vm-bridge down 2>/dev/null || true
ip link delete vm-bridge 2>/dev/null || true
rm -f /etc/netplan/99-vm-bridge.yaml
netplan apply 2>/dev/null || true

echo -e "${YELLOW}[6/9] Removing dnsmasq configuration...${NC}"
rm -f /etc/dnsmasq.d/vm-bridge.conf
systemctl restart dnsmasq 2>/dev/null || true

echo -e "${YELLOW}[7/9] Restoring sysctl settings...${NC}"
if [ -f /etc/sysctl.conf.backup* ]; then
    LATEST_BACKUP=$(ls -t /etc/sysctl.conf.backup* | head -1)
    cp "$LATEST_BACKUP" /etc/sysctl.conf
    sysctl -p
else
    # Just comment out our changes
    sed -i 's/^net.ipv4.ip_forward=1/#net.ipv4.ip_forward=1/' /etc/sysctl.conf
    sed -i 's/^net.ipv6.conf.all.forwarding=1/#net.ipv6.conf.all.forwarding=1/' /etc/sysctl.conf
    sysctl -p
fi

echo -e "${YELLOW}[8/9] Removing installation directory...${NC}"
rm -rf /opt/secure-vm

echo -e "${YELLOW}[9/9] Optionally removing packages...${NC}"
echo ""
echo "The following packages were installed and can be removed:"
echo "  - qemu-kvm libvirt-daemon-system libvirt-clients"
echo "  - bridge-utils virtinst virt-manager"
echo "  - wireguard wireguard-tools"
echo "  - dnsmasq cloud-image-utils genisoimage"
echo ""
echo -n "Remove these packages? (y/N): "
read REMOVE_PKGS

if [ "$REMOVE_PKGS" = "y" ] || [ "$REMOVE_PKGS" = "Y" ]; then
    apt-get remove -y qemu-kvm libvirt-daemon-system libvirt-clients \
        bridge-utils virtinst virt-manager wireguard wireguard-tools \
        cloud-image-utils genisoimage
    apt-get autoremove -y
    echo -e "${GREEN}✓ Packages removed${NC}"
else
    echo "Packages kept (may be useful for other purposes)"
fi

systemctl daemon-reload

echo ""
echo -e "${GREEN}=== Uninstallation Complete ===${NC}"
echo ""
echo "All traces of the secure VM setup have been removed."
echo "Your system has been restored to its original state."
echo ""
echo "If you kept the packages, you can remove them later with:"
echo "  sudo apt-get remove qemu-kvm libvirt-daemon-system wireguard wireguard-tools"
echo "  sudo apt-get autoremove"
