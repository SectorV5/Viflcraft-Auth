#!/bin/bash

# Script to remove unwanted features from Viflcraft Auth
# Features to remove:
# 1. TOTP 2FA
# 2. AutoLogin
# 3. Forbidden passwords
# 4. Automatic data migration for premium players

set -e

echo "Starting feature removal..."

# Step 1: Remove TOTP directory
echo "Step 1: Removing TOTP functionality..."
rm -rf /app/API/src/main/java/win/sectorfive/viflcraftauth/totp
echo "✓ TOTP directory removed"

# Step 2: Remove forbidden-passwords.txt
echo "Step 2: Removing forbidden passwords file..."
rm -f /app/forbidden-passwords.txt
echo "✓ forbidden-passwords.txt removed"

# Step 3: Find and list files that reference TOTP
echo "Step 3: Finding files with TOTP references..."
grep -r "TOTPProvider\|TOTPData\|import.*totp\|getTOTPProvider" /app --include="*.java" | cut -d: -f1 | sort -u > /tmp/totp_files.txt
echo "Found $(wc -l < /tmp/totp_files.txt) files with TOTP references"

# Step 4: Find files with AutoLogin references
echo "Step 4: Finding files with AutoLogin references..."
grep -r "AutoLogin\|autoLogin" /app --include="*.java" | cut -d: -f1 | sort -u > /tmp/autologin_files.txt
echo "Found $(wc -l < /tmp/autologin_files.txt) files with AutoLogin references"

# Step 5: Find files with forbidden password references
echo "Step 5: Finding files with forbidden password references..."
grep -r "forbiddenPassword\|ForbiddenPassword" /app --include="*.java" | cut -d: -f1 | sort -u > /tmp/forbidden_files.txt
echo "Found $(wc -l < /tmp/forbidden_files.txt) files with forbidden password references"

echo "Feature removal preparation complete!"
echo ""
echo "Files requiring manual editing:"
echo "- TOTP references: /tmp/totp_files.txt"
echo "- AutoLogin references: /tmp/autologin_files.txt"  
echo "- Forbidden password references: /tmp/forbidden_files.txt"
