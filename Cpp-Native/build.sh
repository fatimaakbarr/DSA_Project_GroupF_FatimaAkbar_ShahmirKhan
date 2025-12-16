#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Resolve JAVA_HOME if not set
if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVAC_PATH="$(command -v javac)"
  if [[ -z "$JAVAC_PATH" ]]; then
    echo "ERROR: javac not found in PATH" >&2
    exit 1
  fi
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$JAVAC_PATH")")")"
fi

JNI_INCLUDE=("-I${JAVA_HOME}/include" "-I${JAVA_HOME}/include/linux")

OUT_LIB="${ROOT_DIR}/libcampus_backend.so"

echo "JAVA_HOME=${JAVA_HOME}"
echo "Building ${OUT_LIB}"

g++ -std=c++17 -O2 -fPIC -shared \
  "${ROOT_DIR}/native_impl.cpp" \
  "${ROOT_DIR}/graph.cpp" \
  "${ROOT_DIR}/avl_tree.cpp" \
  "${ROOT_DIR}/heap_attendance.cpp" \
  "${ROOT_DIR}/utils_json.cpp" \
  "${JNI_INCLUDE[@]}" \
  -o "${OUT_LIB}"

echo "OK: built ${OUT_LIB}"