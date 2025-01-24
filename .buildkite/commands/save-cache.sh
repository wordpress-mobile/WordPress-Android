#!/bin/bash

set -euo pipefail

echo "--- 📦 Download Dependencies"
./gradlew downloadDependencies androidDependencies
echo ""

echo "--- 💾 Save Cache"
save_gradle_dependency_cache
