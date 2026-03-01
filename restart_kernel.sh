#!/bin/bash
# Script to (re)start the AgentKernel Foreground Service on the device

PACKAGE="com.google.clawminium.kernel"
SERVICE=".KernelService"
ADB_TARGET="192.168.72.155:46869"
USER_ID="10"

echo "Connecting to ADB target: $ADB_TARGET..."
adb connect $ADB_TARGET

echo "Restarting AgentKernel Service for user $USER_ID..."
# Stop the service first to ensure a clean start
adb shell am stopservice --user $USER_ID -n $PACKAGE/$SERVICE
sleep 1
# Start as foreground service
adb shell am start-foreground-service --user $USER_ID -n $PACKAGE/$SERVICE

echo "Fetching internal bridge IP address..."
BRIDGE_IP=$(adb -s $ADB_TARGET shell ip -4 addr show avf_tap_fixed | grep -oP '(?<=inet\s)\d+(\.\d+){3}')

if [ -z "$BRIDGE_IP" ]; then
    echo "Could not find IP on avf_tap_fixed. Showing all IPs:"
    adb -s $ADB_TARGET shell ip -4 addr show | grep 'inet '
else
    echo "AgentKernel SSE endpoint URL: http://$BRIDGE_IP:8080/sse"
fi

echo "Done."
