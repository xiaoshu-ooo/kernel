#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$ROOT/out-kmi-lkm}"
DST="$ROOT/userspace/ksud/bin"
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

# Accepted input layout:
#   $SRC/aarch64/<kmi>_kernelsu.ko
#   $SRC/x86_64/<kmi>_kernelsu.ko
# Also accepts the kernel/build-all.sh naming kernelsu-<kmi>.ko in $SRC/aarch64.
for abi in aarch64 x86_64; do
  mkdir -p "$DST/$abi"
  for kmi in "${KMIS[@]}"; do
    target="$DST/$abi/${kmi}_kernelsu.ko"
    candidates=(
      "$SRC/$abi/${kmi}_kernelsu.ko"
      "$SRC/$abi/kernelsu-${kmi}.ko"
      "$ROOT/kernel/${kmi}_kernelsu.ko"
      "$ROOT/kernel/kernelsu-${kmi}.ko"
      "$ROOT/kernel/out/$kmi/kernelsu.ko"
      "$ROOT/kernel/out-x64/$kmi/kernelsu.ko"
    )
    found=""
    for c in "${candidates[@]}"; do
      if [ -s "$c" ]; then found="$c"; break; fi
    done
    if [ -z "$found" ]; then
      echo "ERROR: missing $abi $kmi"
      echo "  expected: ${kmi}_kernelsu.ko"
      exit 1
    fi
    cp -f "$found" "$target"
    echo "packed $abi/$kmi <- $found"
  done
done

"$ROOT/scripts/check-kmi-assets.sh"
