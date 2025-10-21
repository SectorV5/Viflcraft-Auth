#!/bin/bash

# Start the isolated VM and ensure VPN is running

VM_NAME="isolated-vm"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

echo -e "${GREEN}=== Starting Isolated VM ===${NC}"

# Check and start WireGuard if not running
if ! ip link show wg0 &>/dev/null; then
    echo -e "${YELLOW}Starting WireGuard VPN...${NC}"
    systemctl start wg-airvpn
    sleep 2
    
    if ip link show wg0 &>/dev/null; then
        echo -e "${GREEN}✓ WireGuard VPN is UP${NC}"
    else
        echo -e "${RED}✗ Failed to start WireGuard VPN${NC}"
        echo "  Check logs: journalctl -u wg-airvpn -n 50"
        exit 1
    fi
else
    echo -e "${GREEN}✓ WireGuard VPN already running${NC}"
fi

# Start VM
echo -e "${YELLOW}Starting VM...${NC}"
VM_STATE=$(virsh domstate $VM_NAME 2>/dev/null || echo "undefined")

if [ "$VM_STATE" = "running" ]; then
    echo -e "${GREEN}✓ VM is already running${NC}"
elif [ "$VM_STATE" = "shut off" ] || [ "$VM_STATE" = "paused" ]; then
    virsh start $VM_NAME
    sleep 3
    echo -e "${GREEN}✓ VM started${NC}"
else
    echo -e "${RED}✗ VM is in unexpected state: $VM_STATE${NC}"
    exit 1
fi

# Display status
echo ""
echo -e "${GREEN}=== Status ===${NC}"
echo "WireGuard VPN:"
wg show wg0 2>/dev/null | head -n 5 || echo "  Not running"

echo ""
echo "VM Network:"
virsh domifaddr $VM_NAME 2>/dev/null || echo "  Waiting for IP... (try again in 30 seconds)"

echo ""
echo "To access VM:"
echo "  1. Get IP: sudo virsh domifaddr $VM_NAME"
echo "  2. SSH: ssh vmuser@<VM_IP>"
echo "  3. Check credentials: cat /opt/secure-vm/VM-CREDENTIALS.txt"
