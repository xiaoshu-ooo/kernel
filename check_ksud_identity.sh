#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
ORIG="$ROOT/manager/app/src/main/jniLibs/arm64-v8a/libksud.so"
NEW="${1:-$ORIG}"
PKG="$(sed -n 's/^KSU_PACKAGE_NAME=//p' "$ROOT/manager/gradle.properties" | head -n1)"
PKG="${PKG:-com.coloros.calculator}"

for f in "$ORIG" "$NEW"; do
  [ -f "$f" ] || { echo "[!] Missing: $f"; exit 1; }
done

echo "=== KernelSU Calculator ksud identity check ==="
echo "Package: $PKG"
for f in "$ORIG" "$NEW"; do
  echo
  echo "--- $f ---"
  sha256sum "$f"
  file "$f" || true
  echo "package marker:"
  strings -a "$f" | grep -F -m1 "$PKG" || echo "NOT FOUND"
  echo "KernelSU marker:"
  strings -a "$f" | grep -E -m3 'KernelSU|ksud|boot-patch|boot_patch' || true
done

echo
echo "=== Byte-for-byte comparison ==="
if cmp -s "$ORIG" "$NEW"; then
  echo "EXACT MATCH: rebuilt binary is identical to the ZIP's original ARM64 libksud.so"
else
  echo "DIFFERENT BINARY: compare symbols/build settings before judging source identity"
fi
