#!/bin/bash

# Script to migrate LibreLogin to Viflcraft Auth
# This script will:
# 1. Rename packages from xyz.kyngs.librelogin to win.sectorfive.viflcraftauth
# 2. Create new directory structure
# 3. Move files to new locations
# 4. Update all imports and references

set -e

echo "Starting migration from LibreLogin to Viflcraft Auth..."

# Step 1: Rename packages in place (update all imports and package declarations)
echo "Step 1: Updating package declarations and imports in Java files..."

find /app -name "*.java" -type f -exec sed -i 's/package xyz\.kyngs\.librelogin/package win.sectorfive.viflcraftauth/g' {} \;
find /app -name "*.java" -type f -exec sed -i 's/import xyz\.kyngs\.librelogin/import win.sectorfive.viflcraftauth/g' {} \;

# Step 2: Update references in other files
echo "Step 2: Updating references in configuration and build files..."

# Update gradle files
find /app -name "*.gradle" -type f -exec sed -i 's/xyz\.kyngs\.librelogin/win.sectorfive.viflcraftauth/g' {} \;
find /app -name "*.gradle.kts" -type f -exec sed -i 's/xyz\.kyngs\.librelogin/win.sectorfive.viflcraftauth/g' {} \;

# Update YAML files
find /app -name "*.yml" -type f -exec sed -i 's/xyz\.kyngs\.librelogin/win.sectorfive.viflcraftauth/g' {} \;
find /app -name "*.yaml" -type f -exec sed -i 's/xyz\.kyngs\.librelogin/win.sectorfive.viflcraftauth/g' {} \;

# Update JSON files
find /app -name "*.json" -type f -exec sed -i 's/xyz\.kyngs\.librelogin/win.sectorfive.viflcraftauth/g' {} \;

# Step 3: Move directory structure
echo "Step 3: Reorganizing directory structure..."

# API module
if [ -d "/app/API/src/main/java/xyz" ]; then
    mkdir -p /app/API/src/main/java/win/sectorfive/viflcraftauth
    if [ -d "/app/API/src/main/java/xyz/kyngs/librelogin/api" ]; then
        mv /app/API/src/main/java/xyz/kyngs/librelogin/api/* /app/API/src/main/java/win/sectorfive/viflcraftauth/ 2>/dev/null || true
    fi
    rm -rf /app/API/src/main/java/xyz
fi

# Plugin module
if [ -d "/app/Plugin/src/main/java/xyz" ]; then
    mkdir -p /app/Plugin/src/main/java/win/sectorfive/viflcraftauth
    if [ -d "/app/Plugin/src/main/java/xyz/kyngs/librelogin" ]; then
        # Move common directory if exists
        if [ -d "/app/Plugin/src/main/java/xyz/kyngs/librelogin/common" ]; then
            mv /app/Plugin/src/main/java/xyz/kyngs/librelogin/common /app/Plugin/src/main/java/win/sectorfive/viflcraftauth/ 2>/dev/null || true
        fi
        # Move paper directory if exists
        if [ -d "/app/Plugin/src/main/java/xyz/kyngs/librelogin/paper" ]; then
            mv /app/Plugin/src/main/java/xyz/kyngs/librelogin/paper /app/Plugin/src/main/java/win/sectorfive/viflcraftauth/ 2>/dev/null || true
        fi
        # Move velocity directory if exists
        if [ -d "/app/Plugin/src/main/java/xyz/kyngs/librelogin/velocity" ]; then
            mv /app/Plugin/src/main/java/xyz/kyngs/librelogin/velocity /app/Plugin/src/main/java/win/sectorfive/viflcraftauth/ 2>/dev/null || true
        fi
        # Move bungeecord directory if exists
        if [ -d "/app/Plugin/src/main/java/xyz/kyngs/librelogin/bungeecord" ]; then
            mv /app/Plugin/src/main/java/xyz/kyngs/librelogin/bungeecord /app/Plugin/src/main/java/win/sectorfive/viflcraftauth/ 2>/dev/null || true
        fi
    fi
    rm -rf /app/Plugin/src/main/java/xyz
fi

echo "Migration complete!"
echo "Verifying package structure..."

# Verify new structure
if [ -d "/app/API/src/main/java/win/sectorfive/viflcraftauth" ]; then
    echo "✓ API module restructured successfully"
else
    echo "✗ API module restructuring failed"
fi

if [ -d "/app/Plugin/src/main/java/win/sectorfive/viflcraftauth" ]; then
    echo "✓ Plugin module restructured successfully"
else
    echo "✗ Plugin module restructuring failed"
fi

echo "Package migration completed!"
