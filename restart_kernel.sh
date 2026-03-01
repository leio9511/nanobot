#!/bin/bash
# Script to (re)start the AgentKernel Foreground Service on the device

PACKAGE="com.google.clawminium.kernel"
SERVICE=".KernelService"
ADB_TARGET="192.168.72.155:46869"

echo "Connecting to ADB target: $ADB_TARGET..."
adb connect $ADB_TARGET

echo "Restarting AgentKernel Service..."
# Stop the service first to ensure a clean start
adb shell am stopservice -n $PACKAGE/$SERVICE
sleep 1
# Start as foreground service
adb shell am start-foreground-service -n $PACKAGE/$SERVICE

echo "Done."
