#!/bin/bash
# Script to (re)start the Nanobot Gateway on the Linux VM

# Kill existing instances
echo "Stopping existing Nanobot instances..."
pkill -f "nanobot gateway" || true
sleep 1

echo "Starting Nanobot Gateway via run_gateway.sh..."
# Start the run_gateway.sh script which has the auto-restart loop
bash run_gateway.sh
