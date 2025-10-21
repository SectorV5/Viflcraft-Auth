#!/bin/bash

# Deploy script - uploads all files to VPS and runs installation

VPS_IP="91.98.80.35"
VPS_USER="root"
SSH_KEY="./id_rsa"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Deploying Secure VM Setup to VPS ===${NC}"
echo ""

# Check if SSH key exists
if [ ! -f "$SSH_KEY" ]; then
    echo -e "${RED}Error: SSH key not found: $SSH_KEY${NC}"
    exit 1
fi

chmod 600 $SSH_KEY

# Test SSH connection
echo -e "${YELLOW}Testing SSH connection...${NC}"
if ! ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i $SSH_KEY $VPS_USER@$VPS_IP "echo 'Connection OK'" 2>/dev/null; then
    echo -e "${RED}Error: Cannot connect to VPS${NC}"
    exit 1
fi
echo -e "${GREEN}✓ SSH connection successful${NC}"
echo ""

# Create remote directory
echo -e "${YELLOW}Creating remote directory...${NC}"
ssh -i $SSH_KEY $VPS_USER@$VPS_IP "mkdir -p /root/vm-setup-files"

# Upload all files
echo -e "${YELLOW}Uploading files...${NC}"
scp -i $SSH_KEY install-vm.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY AirVPN.conf $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY vm-start.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY vm-stop.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY vm-delete.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY vpn-change.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY vpn-status.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY uninstall.sh $VPS_USER@$VPS_IP:/root/vm-setup-files/
scp -i $SSH_KEY README.md $VPS_USER@$VPS_IP:/root/vm-setup-files/

echo -e "${GREEN}✓ Files uploaded${NC}"
echo ""

# Make scripts executable
echo -e "${YELLOW}Setting permissions...${NC}"
ssh -i $SSH_KEY $VPS_USER@$VPS_IP "chmod +x /root/vm-setup-files/*.sh"

echo -e "${GREEN}✓ Files ready on VPS${NC}"
echo ""
echo "Files uploaded to: /root/vm-setup-files/"
echo ""
echo "Next steps:"
echo "  1. SSH to VPS: ssh -i $SSH_KEY $VPS_USER@$VPS_IP"
echo "  2. cd /root/vm-setup-files"
echo "  3. sudo ./install-vm.sh"
echo ""
echo -e "${YELLOW}Would you like to run the installation now? (y/N)${NC}"
read -r RUN_NOW

if [ "$RUN_NOW" = "y" ] || [ "$RUN_NOW" = "Y" ]; then
    echo ""
    echo -e "${GREEN}Starting installation on VPS...${NC}"
    echo -e "${YELLOW}This will take 5-15 minutes. Please wait...${NC}"
    echo ""
    
    ssh -i $SSH_KEY $VPS_USER@$VPS_IP "cd /root/vm-setup-files && sudo ./install-vm.sh"
    
    echo ""
    echo -e "${GREEN}=== Deployment Complete ===${NC}"
    echo ""
    echo "To get VM credentials:"
    echo "  ssh -i $SSH_KEY $VPS_USER@$VPS_IP 'sudo cat /opt/secure-vm/VM-CREDENTIALS.txt'"
else
    echo ""
    echo "Installation not started. Run manually when ready:"
    echo "  ssh -i $SSH_KEY $VPS_USER@$VPS_IP"
    echo "  cd /root/vm-setup-files"
    echo "  sudo ./install-vm.sh"
fi
