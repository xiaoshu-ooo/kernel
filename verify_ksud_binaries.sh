#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
A="$ROOT/manager/app/src/main/jniLibs/arm64-v8a/libksud.so"
X="$ROOT/manager/app/src/main/jniLibs/x86_64/libksud.so"
PKG="$(sed -n 's/^KSU_PACKAGE_NAME=//p' "$ROOT/manager/gradle.properties" | head -n1)"
PKG="${PKG:-com.coloros.calculator}"
for f in "$A" "$X"; do
  test -f "$f" || { echo "[FAIL] missing $f"; exit 1; }
  echo "=== $f ==="
  file "$f"
  strings -a "$f" | grep -F -q "$PKG" || { echo "[FAIL] package marker missing: $PKG"; exit 1; }
  for kmi in \
    android12-5.10 android13-5.10 android13-5.15 android14-5.15 \
    android14-6.1 android15-6.6 android16-6.12 android17-6.18; do
    strings -a "$f" | grep -F -q "${kmi}_kernelsu.ko" || { echo "[FAIL] missing embedded ${kmi}_kernelsu.ko"; exit 1; }
  done
  echo "[OK] package + all 8 embedded KMI modules found"
done
echo "=== BOOT PATCH SOURCE ==="
grep -q 'Commands::BootPatch' "$ROOT/userspace/ksud/src/cli.rs"
grep -q 'Commands::BootPatchV2' "$ROOT/userspace/ksud/src/cli.rs"
grep -q 'cpio.add("kernelsu.ko"' "$ROOT/userspace/ksud/src/boot_patch.rs"
echo "[OK] boot/init_boot patch code present"
