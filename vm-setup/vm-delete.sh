#!/bin/bash

# Delete the VM completely (keeps VPN and infrastructure)

VM_NAME="isolated-vm"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

echo -e "${RED}=== Delete VM ===${NC}"
echo "This will PERMANENTLY delete the VM and all its data!"
echo -n "Are you sure? (type 'yes' to confirm): "
read CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Aborted."
    exit 0
fi

echo -e "${YELLOW}Stopping VM...${NC}"
virsh destroy $VM_NAME 2>/dev/null || true

echo -e "${YELLOW}Undefining VM...${NC}"
virsh undefine $VM_NAME --remove-all-storage 2>/dev/null || true

echo -e "${YELLOW}Removing VM data...${NC}"
rm -f /opt/secure-vm/vm-data/${VM_NAME}.qcow2
rm -f /opt/secure-vm/vm-data/cloud-init.iso
rm -f /opt/secure-vm/vm-data/user-data
rm -f /opt/secure-vm/vm-data/meta-data
rm -f /opt/secure-vm/VM-CREDENTIALS.txt

echo -e "${GREEN}✓ VM deleted${NC}"
echo ""
echo "The VPN and network infrastructure are still in place."
echo "To create a new VM, run: /opt/secure-vm/scripts/create-new-vm.sh"
