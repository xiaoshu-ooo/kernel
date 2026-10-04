#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# KernelSU calculator build helper.
# Environment:
#   KSU_MANAGER_PACKAGE  Manager package to bind in kernel (default: com.coloros.calculator)
#   KSU_EXPECTED_SIZE    DER certificate length in bytes
#   KSU_EXPECTED_HASH    SHA-256 of DER certificate
#   KSU_EXPECTED_SIZE2 / KSU_EXPECTED_HASH2  optional second signer
#   KSU_KMIS             space-separated KMI list

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

KSU_MANAGER_PACKAGE="${KSU_MANAGER_PACKAGE:-com.coloros.calculator}"
KSU_EXPECTED_SIZE="${KSU_EXPECTED_SIZE:-}"
KSU_EXPECTED_HASH="${KSU_EXPECTED_HASH:-}"
KSU_KMIS="${KSU_KMIS:-android12-5.10 android13-5.10 android13-5.15 android14-5.15 android14-6.1 android15-6.6 android16-6.12 android17-6.18}"

if [[ -z "$KSU_EXPECTED_SIZE" || -z "$KSU_EXPECTED_HASH" ]]; then
    echo "[!] KSU_EXPECTED_SIZE and KSU_EXPECTED_HASH must be supplied."
    echo "    They must come from the final Manager APK certificate."
    exit 2
fi

[[ "$KSU_EXPECTED_HASH" =~ ^[0-9a-fA-F]{64}$ ]] || {
    echo "[!] Invalid KSU_EXPECTED_HASH: $KSU_EXPECTED_HASH"; exit 2;
}
[[ "$KSU_EXPECTED_SIZE" =~ ^[0-9]+$ ]] || {
    echo "[!] Invalid KSU_EXPECTED_SIZE: $KSU_EXPECTED_SIZE"; exit 2;
}

mv .ddk-version .ddk-version.bak 2>/dev/null || true
restore_ddk() { mv .ddk-version.bak .ddk-version 2>/dev/null || true; }
trap restore_ddk EXIT

# Environment variables are inherited by the DDK/Kbuild invocation and become
# Make variables, so Kbuild's KSU_MANAGER_PACKAGE/EXPECTED_* are deterministic.
export KSU_MANAGER_PACKAGE KSU_EXPECTED_SIZE KSU_EXPECTED_HASH
if [[ -n "${KSU_EXPECTED_SIZE2:-}" || -n "${KSU_EXPECTED_HASH2:-}" ]]; then
    [[ -n "${KSU_EXPECTED_SIZE2:-}" && -n "${KSU_EXPECTED_HASH2:-}" ]] || {
        echo "[!] KSU_EXPECTED_SIZE2 and KSU_EXPECTED_HASH2 must be set together."; exit 2;
    }
    export KSU_EXPECTED_SIZE2 KSU_EXPECTED_HASH2
fi

for kmi in $KSU_KMIS; do
    echo "========== Building $kmi =========="
    ODIR="$ROOT/out/$kmi"
    mkdir -p "$ODIR"
    if ddk build "$kmi" "ODIR=$ODIR" "KSU_MANAGER_PACKAGE=$KSU_MANAGER_PACKAGE" "KSU_EXPECTED_SIZE=$KSU_EXPECTED_SIZE" "KSU_EXPECTED_HASH=$KSU_EXPECTED_HASH" -e CONFIG_KSU=m; then
        if [[ -f "$ODIR/kernelsu.ko" ]]; then
            cp -f "$ODIR/kernelsu.ko" "$ROOT/kernelsu-${kmi}.ko"
            llvm-strip -d "$ROOT/kernelsu-${kmi}.ko"
            echo "[OK] kernelsu-${kmi}.ko"
        else
            echo "[!] DDK reported success but kernelsu.ko is missing: $ODIR" >&2
            exit 1
        fi
    else
        echo "[!] Build failed for $kmi" >&2
        exit 1
    fi
done

echo
echo "========== KernelSU identity =========="
echo "Manager package : $KSU_MANAGER_PACKAGE"
echo "Cert size       : $KSU_EXPECTED_SIZE bytes"
echo "Cert SHA-256    : $KSU_EXPECTED_HASH"
echo
echo "========== Final output =========="
ls -lh "$ROOT"/kernelsu-*.ko
