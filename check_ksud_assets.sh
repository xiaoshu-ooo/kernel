#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
for arch in aarch64 x86_64; do
  f="$ROOT/userspace/ksud/bin/$arch"
  echo "===== $arch assets ====="
  test -x "$f/ksuinit" || { echo "MISSING: $f/ksuinit"; exit 1; }
  for kmi in android12-5.10 android13-5.10 android13-5.15 android14-5.15 android14-6.1 android15-6.6 android16-6.12 android17-6.18; do
    test -f "$f/${kmi}_kernelsu.ko" || { echo "MISSING: $f/${kmi}_kernelsu.ko"; exit 1; }
  done
  echo "OK"
done
