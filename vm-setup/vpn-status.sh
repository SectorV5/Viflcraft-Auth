#!/bin/bash

# Check VPN and VM status

VM_NAME="isolated-vm"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

echo -e "${GREEN}=== VPN and VM Status ===${NC}"
echo ""

# WireGuard Status
echo "WireGuard VPN:"
echo "=============="
if ip link show wg0 &>/dev/null; then
    echo -e "${GREEN}✓ Status: ACTIVE${NC}"
    VPN_IP=$(ip -4 addr show wg0 | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | head -1)
    echo "  VPN IP: $VPN_IP"
    echo ""
    wg show wg0
    echo ""
    
    # Test VPN connectivity
    echo "Testing VPN connectivity..."
    if timeout 5 ping -c 1 -I wg0 8.8.8.8 &>/dev/null; then
        echo -e "${GREEN}✓ VPN has internet connectivity${NC}"
    else
        echo -e "${RED}✗ VPN connectivity test failed${NC}"
    fi
else
    echo -e "${RED}✗ Status: NOT RUNNING${NC}"
    echo "  Start with: sudo systemctl start wg-airvpn"
fi

echo ""
echo "VM Status:"
echo "=========="
VM_STATE=$(virsh domstate $VM_NAME 2>/dev/null || echo "undefined")

if [ "$VM_STATE" = "running" ]; then
    echo -e "${GREEN}✓ VM is RUNNING${NC}"
    echo ""
    echo "VM Network Info:"
    virsh domifaddr $VM_NAME 2>/dev/null || echo "  IP not available yet"
    echo ""
    
    # Check if VM can reach internet through VPN
    VM_IP=$(virsh domifaddr $VM_NAME 2>/dev/null | grep -oP '10\.10\.10\.\d+' | head -1)
    if [ -n "$VM_IP" ]; then
        echo "VM IP: $VM_IP"
        echo ""
        echo "Testing VM connectivity (from host)..."
        if timeout 3 ping -c 1 $VM_IP &>/dev/null; then
            echo -e "${GREEN}✓ VM is reachable from host${NC}"
        else
            echo -e "${YELLOW}⚠ VM not responding to ping${NC}"
        fi
    fi
elif [ "$VM_STATE" = "shut off" ]; then
    echo -e "${YELLOW}⚠ VM is STOPPED${NC}"
    echo "  Start with: sudo /opt/secure-vm/scripts/vm-start.sh"
else
    echo -e "${RED}✗ VM state: $VM_STATE${NC}"
fi

echo ""
echo "Firewall Rules (Kill Switch):"
echo "=============================="
echo "Rules preventing VM from bypassing VPN:"
iptables -L FORWARD -n -v | grep -A 5 "vm-bridge" || echo "  No rules found"

echo ""
echo "Bridge Network:"
echo "==============="
ip addr show vm-bridge 2>/dev/null || echo "  Bridge not found"

echo ""
echo "Quick Access:"
echo "============="
echo "  View credentials: sudo cat /opt/secure-vm/VM-CREDENTIALS.txt"
echo "  Start VM: sudo /opt/secure-vm/scripts/vm-start.sh"
echo "  Stop VM: sudo /opt/secure-vm/scripts/vm-stop.sh"
echo "  VPN logs: sudo journalctl -u wg-airvpn -n 50"
