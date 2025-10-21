#!/bin/bash

# Stop the isolated VM (but keep VPN running)

VM_NAME="isolated-vm"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

echo -e "${GREEN}=== Stopping Isolated VM ===${NC}"

VM_STATE=$(virsh domstate $VM_NAME 2>/dev/null || echo "undefined")

if [ "$VM_STATE" = "running" ]; then
    echo -e "${YELLOW}Shutting down VM gracefully...${NC}"
    virsh shutdown $VM_NAME
    
    # Wait up to 30 seconds for graceful shutdown
    for i in {1..30}; do
        sleep 1
        VM_STATE=$(virsh domstate $VM_NAME 2>/dev/null)
        if [ "$VM_STATE" = "shut off" ]; then
            echo -e "${GREEN}✓ VM shut down successfully${NC}"
            exit 0
        fi
    done
    
    # Force shutdown if still running
    echo -e "${YELLOW}Force stopping VM...${NC}"
    virsh destroy $VM_NAME
    echo -e "${GREEN}✓ VM stopped${NC}"
elif [ "$VM_STATE" = "shut off" ]; then
    echo -e "${GREEN}✓ VM is already stopped${NC}"
else
    echo -e "${YELLOW}⚠ VM state: $VM_STATE${NC}"
fi

echo ""
echo "Note: WireGuard VPN is still running."
echo "To stop VPN: sudo systemctl stop wg-airvpn"
