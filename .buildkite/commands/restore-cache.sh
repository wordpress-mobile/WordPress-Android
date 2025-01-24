#!/bin/bash -e

echo "--- 💾 Restore Cache"
restore_gradle_dependency_cache || true
