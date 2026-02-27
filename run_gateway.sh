#!/bin/bash
while true; do
    echo "Starting nanobot gateway..."
    uv run nanobot gateway
    echo "Nanobot gateway crashed. Restarting in 5 seconds..."
    sleep 5
done
