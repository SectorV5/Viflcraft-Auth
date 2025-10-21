#!/bin/bash

# Test script to verify host connectivity is NOT affected by VM setup

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Host Connectivity Test ===${NC}"
echo "This verifies the host OS remains accessible after VM setup"
echo ""

# Test 1: Check main network interface
echo -e "${YELLOW}[Test 1] Main network interface...${NC}"
MAIN_IF=$(ip route | grep default | awk '{print $5}' | head -1)
MAIN_IP=$(ip -4 addr show $MAIN_IF | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | head -1)

if [ -n "$MAIN_IF" ] && [ -n "$MAIN_IP" ]; then
    echo -e "${GREEN}✓ Main interface: $MAIN_IF${NC}"
    echo -e "${GREEN}✓ Main IP: $MAIN_IP${NC}"
else
    echo -e "${RED}✗ Could not detect main interface${NC}"
fi
echo ""

# Test 2: Check default route
echo -e "${YELLOW}[Test 2] Default route...${NC}"
DEFAULT_GW=$(ip route | grep default | awk '{print $3}' | head -1)
DEFAULT_IF=$(ip route | grep default | awk '{print $5}' | head -1)

if [ -n "$DEFAULT_GW" ]; then
    echo -e "${GREEN}✓ Default gateway: $DEFAULT_GW via $DEFAULT_IF${NC}"
    
    # Make sure it's NOT wg0
    if [ "$DEFAULT_IF" = "wg0" ]; then
        echo -e "${RED}✗ WARNING: Default route is through wg0!${NC}"
        echo -e "${RED}  This will break host SSH access!${NC}"
        echo -e "${YELLOW}  Run: ip route del default via $DEFAULT_GW dev wg0${NC}"
        echo -e "${YELLOW}  Then add correct route back via your main interface${NC}"
    else
        echo -e "${GREEN}✓ Default route is NOT through VPN${NC}"
    fi
else
    echo -e "${RED}✗ No default route found${NC}"
fi
echo ""

# Test 3: Check if WireGuard is capturing all traffic
echo -e "${YELLOW}[Test 3] WireGuard routing table...${NC}"
if ip link show wg0 &>/dev/null; then
    echo "WireGuard interface exists, checking routes..."
    WG_ROUTES=$(ip route show dev wg0)
    
    if echo "$WG_ROUTES" | grep -q "default"; then
        echo -e "${RED}✗ WARNING: WireGuard has default route!${NC}"
        echo -e "${RED}  This will break host connectivity!${NC}"
        echo "$WG_ROUTES"
    elif echo "$WG_ROUTES" | grep -q "10.10.10.0/24"; then
        echo -e "${GREEN}✓ WireGuard only routes VM subnet (10.10.10.0/24)${NC}"
        echo "$WG_ROUTES"
    else
        echo "WireGuard routes:"
        echo "$WG_ROUTES"
    fi
else
    echo "WireGuard interface not active"
fi
echo ""

# Test 4: DNS Resolution
echo -e "${YELLOW}[Test 4] DNS resolution...${NC}"
if host google.com &>/dev/null; then
    echo -e "${GREEN}✓ DNS working${NC}"
else
    echo -e "${RED}✗ DNS not working${NC}"
fi
echo ""

# Test 5: Internet connectivity
echo -e "${YELLOW}[Test 5] Internet connectivity...${NC}"
if timeout 5 ping -c 1 8.8.8.8 &>/dev/null; then
    echo -e "${GREEN}✓ Can ping 8.8.8.8${NC}"
else
    echo -e "${RED}✗ Cannot ping 8.8.8.8${NC}"
fi

if timeout 5 curl -s ifconfig.me &>/dev/null; then
    PUBLIC_IP=$(timeout 5 curl -s ifconfig.me)
    echo -e "${GREEN}✓ Public IP: $PUBLIC_IP${NC}"
    
    # Check if it matches our main IP or VPS IP
    if [ "$PUBLIC_IP" = "$MAIN_IP" ] || [ "$PUBLIC_IP" = "91.98.80.35" ]; then
        echo -e "${GREEN}✓ Host is using its own IP (not VPN)${NC}"
    else
        echo -e "${YELLOW}⚠ Host might be using VPN IP: $PUBLIC_IP${NC}"
    fi
else
    echo -e "${RED}✗ Cannot reach internet${NC}"
fi
echo ""

# Test 6: Firewall rules
echo -e "${YELLOW}[Test 6] Firewall configuration...${NC}"
FORWARD_POLICY=$(iptables -L FORWARD | head -1 | awk '{print $4}')
echo "FORWARD chain policy: $FORWARD_POLICY"

if [ "$FORWARD_POLICY" = "DROP" ]; then
    echo -e "${YELLOW}⚠ FORWARD policy is DROP - ensure host-to-host forwarding still works${NC}"
else
    echo -e "${GREEN}✓ FORWARD policy allows traffic${NC}"
fi

# Check for VM-specific rules
VM_RULES=$(iptables -L FORWARD -n | grep -c "vm-bridge")
if [ "$VM_RULES" -gt 0 ]; then
    echo -e "${GREEN}✓ Found $VM_RULES VM-specific firewall rules${NC}"
else
    echo "No VM-specific firewall rules found"
fi
echo ""

# Test 7: SSH Port accessibility
echo -e "${YELLOW}[Test 7] SSH port (22) status...${NC}"
if ss -tln | grep -q ":22 "; then
    echo -e "${GREEN}✓ SSH port 22 is listening${NC}"
else
    echo -e "${RED}✗ SSH port 22 not listening${NC}"
fi
echo ""

# Summary
echo -e "${GREEN}=== Summary ===${NC}"
echo ""
echo "Expected configuration:"
echo "  • Host has normal internet access via $MAIN_IF"
echo "  • Host is NOT routing through VPN"
echo "  • SSH on port 22 remains accessible"
echo "  • Only VM traffic (10.10.10.0/24) routes through wg0"
echo ""

# Final check
if ip route | grep default | grep -q wg0; then
    echo -e "${RED}⚠ CRITICAL: Default route goes through wg0!${NC}"
    echo -e "${RED}This WILL break SSH access from outside!${NC}"
    echo ""
    echo "To fix immediately:"
    echo "  1. Remove wrong default route: ip route del default"
    echo "  2. Add correct route: ip route add default via <your-gateway> dev $MAIN_IF"
    echo "  3. Check: ip route show"
else
    echo -e "${GREEN}✓ Host connectivity should be preserved${NC}"
fi
