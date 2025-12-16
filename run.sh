#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${ROOT}/Cpp-Native/build.sh"

javac "${ROOT}/SCNS-Java/src"/*.java

# Run from repo root so NativeBridge loads ./Cpp-Native/libcampus_backend.so
exec java -cp "${ROOT}/SCNS-Java/src" MainMenu
