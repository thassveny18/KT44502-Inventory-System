#!/usr/bin/env bash
# StockAtom - runs the whole project on Linux:
# environment info, unit tests, then the concurrent simulation.
set -e
cd "$(dirname "$0")"

echo "=== Environment ==="
uname -a
(lsb_release -a 2>/dev/null || cat /etc/os-release)
java -version 2>&1 | head -n 1
lein version

echo
echo "=== Unit tests ==="
lein test

echo
echo "=== Simulation ==="
lein run