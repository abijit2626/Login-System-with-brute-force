#!/bin/bash

# Change to the directory where the script is located
cd "$(dirname "$0")" || exit

# Check if java is installed
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in your PATH."
    echo "Please install Java 11 or newer to run this console."
    echo "Opening https://adoptium.net ..."
    if command -v xdg-open &> /dev/null; then xdg-open https://adoptium.net
    elif command -v open &> /dev/null; then open https://adoptium.net
    fi
    exit 1
fi

# Verify Java version (11+)
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
JAVA_MAJOR=$(echo "$JAVA_VERSION" | awk -F '.' '{if ($1 == 1) print $2; else print $1}')

if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 11 ]; then
    echo "[ERROR] Java 11 or newer is required. You are running version $JAVA_VERSION."
    echo "Opening https://adoptium.net ..."
    if command -v xdg-open &> /dev/null; then xdg-open https://adoptium.net
    elif command -v open &> /dev/null; then open https://adoptium.net
    fi
    exit 1
fi

echo "Launching Unified Security Console..."
java -jar "../target/unified-console-1.0-SNAPSHOT.jar"

if [ $? -ne 0 ]; then
    echo "[ERROR] Application crashed or failed to launch. Check app_crash.log."
    read -p "Press Enter to continue..."
fi
