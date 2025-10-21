#!/bin/bash

# Change VPN configuration

VPN_CONFIG="/opt/secure-vm/wireguard/airvpn.conf"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Error: This script must be run as root${NC}"
    exit 1
fi

echo -e "${GREEN}=== Change VPN Configuration ===${NC}"
echo ""
echo "Current VPN config: $VPN_CONFIG"
echo ""
echo "Options:"
echo "  1. Replace with new WireGuard config file"
echo "  2. Edit current config manually"
echo "  3. View current config"
echo "  4. Cancel"
echo ""
echo -n "Choose option (1-4): "
read OPTION

case $OPTION in
    1)
        echo -n "Enter path to new WireGuard config file: "
        read NEW_CONFIG
        
        if [ ! -f "$NEW_CONFIG" ]; then
            echo -e "${RED}Error: File not found: $NEW_CONFIG${NC}"
            exit 1
        fi
        
        # Backup old config
        cp $VPN_CONFIG ${VPN_CONFIG}.backup.$(date +%Y%m%d_%H%M%S)
        
        # Copy new config
        cp "$NEW_CONFIG" $VPN_CONFIG
        chmod 600 $VPN_CONFIG
        
        # Add PostUp/PostDown rules for kill switch
        if ! grep -q "PostUp" $VPN_CONFIG; then
            echo "" >> $VPN_CONFIG
            echo "PostUp = iptables -A FORWARD -i vm-bridge -o wg0 -j ACCEPT; iptables -A FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT; iptables -t nat -A POSTROUTING -o wg0 -j MASQUERADE; iptables -A FORWARD -i vm-bridge ! -o wg0 -j REJECT" >> $VPN_CONFIG
            echo "PostDown = iptables -D FORWARD -i vm-bridge -o wg0 -j ACCEPT; iptables -D FORWARD -i wg0 -o vm-bridge -m state --state RELATED,ESTABLISHED -j ACCEPT; iptables -t nat -D POSTROUTING -o wg0 -j MASQUERADE; iptables -D FORWARD -i vm-bridge ! -o wg0 -j REJECT" >> $VPN_CONFIG
        fi
        
        echo -e "${GREEN}✓ VPN config updated${NC}"
        echo ""
        echo "Restarting VPN..."
        systemctl restart wg-airvpn
        sleep 2
        
        if ip link show wg0 &>/dev/null; then
            echo -e "${GREEN}✓ VPN restarted successfully${NC}"
            wg show wg0
        else
            echo -e "${RED}✗ VPN failed to start with new config${NC}"
            echo "  Check logs: journalctl -u wg-airvpn -n 50"
            echo "  Restoring backup..."
            mv ${VPN_CONFIG}.backup.* $VPN_CONFIG
            systemctl restart wg-airvpn
        fi
        ;;
        
    2)
        # Backup before editing
        cp $VPN_CONFIG ${VPN_CONFIG}.backup.$(date +%Y%m%d_%H%M%S)
        
        ${EDITOR:-nano} $VPN_CONFIG
        
        echo ""
        echo "Restarting VPN with new config..."
        systemctl restart wg-airvpn
        sleep 2
        
        if ip link show wg0 &>/dev/null; then
            echo -e "${GREEN}✓ VPN restarted successfully${NC}"
        else
            echo -e "${RED}✗ VPN failed to start${NC}"
            echo "  Check logs: journalctl -u wg-airvpn -n 50"
        fi
        ;;
        
    3)
        echo ""
        echo "Current VPN configuration:"
        echo "=========================="
        cat $VPN_CONFIG | grep -v "PrivateKey\|PresharedKey" || cat $VPN_CONFIG
        ;;
        
    4)
        echo "Cancelled."
        exit 0
        ;;
        
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac
