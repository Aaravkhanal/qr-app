#!/usr/bin/env bash
set -euo pipefail

tracked_patterns=("*.kt" "*.kts" "*.xml" "*.gradle" "*.yml" "*.yaml")

if git grep -nI -E '[[:blank:]]+$' -- "${tracked_patterns[@]}"; then
    echo "Formatting check failed: trailing whitespace found."
    exit 1
fi

if git grep -nI $'\r' -- "${tracked_patterns[@]}"; then
    echo "Formatting check failed: CRLF line endings found."
    exit 1
fi

forbidden_files="$(git ls-files -- '.gradle/**' ':(glob)**/build/**' '*.jks' '*.keystore' 'keystore.properties')"
if [[ -n "$forbidden_files" ]]; then
    echo "Repository hygiene check failed: generated files or signing material are tracked:"
    echo "$forbidden_files"
    exit 1
fi

echo "Formatting checks passed."
