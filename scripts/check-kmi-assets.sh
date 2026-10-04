#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KSU_BIN="$ROOT/userspace/ksud/bin"
KMIS=(
  android12-5.10
  android13-5.10
  android13-5.15
  android14-5.15
  android14-6.1
  android15-6.6
  android16-6.12
  android17-6.18
)
FAIL=0
for abi in aarch64 x86_64; do
  echo "== $abi =="
  for kmi in "${KMIS[@]}"; do
    f="$KSU_BIN/$abi/${kmi}_kernelsu.ko"
    if [ -s "$f" ]; then
      printf 'OK   %s  %s bytes\n' "$kmi" "$(wc -c < "$f")"
    else
      echo "MISS $kmi -> $f"
      FAIL=1
    fi
  done
done
if [ "$FAIL" -ne 0 ]; then
  echo
  echo "Missing KMI LKM assets. Build them with .github/workflows/build-lkm.yml or kernel/build-all.sh/build-all-x64.sh, then run scripts/pack-kmi-assets.sh."
  exit 1
fi
echo "All 8 KMI assets are present for both ABIs."
