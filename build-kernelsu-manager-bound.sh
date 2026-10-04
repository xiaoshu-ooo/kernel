#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# Build ONLY the KernelSU kernel module with the Manager identity bound to
# com.coloros.calculator. EXPECTED_SIZE/HASH are derived automatically from the
# exact Manager APK supplied to this script.

ROOT="$(cd "$(dirname "$0")" && pwd)"
CERT_TOOL="$ROOT/scripts/apk_v2_cert.py"
PACKAGE="com.coloros.calculator"
APK="${1:-}"
KMI="${2:-${KSU_KMI:-android15-6.6}}"

if [[ -z "$APK" ]]; then
  echo "用法: $0 <com.coloros.calculator Manager.apk> [KMI]"
  echo "示例: $0 ~/Manager.apk android15-6.6"
  exit 2
fi
APK="$(realpath "$APK")"
[[ -f "$APK" ]] || { echo "[!] APK 不存在: $APK"; exit 1; }
[[ -f "$CERT_TOOL" ]] || { echo "[!] 找不到证书解析脚本: $CERT_TOOL"; exit 1; }

readarray -t CERT < <(python3 "$CERT_TOOL" "$APK")
CERT_SIZE="${CERT[0]#SIZE=}"
CERT_HASH="${CERT[1]#SHA256=}"
[[ "$CERT_SIZE" =~ ^[0-9]+$ ]] || { echo "[!] 无法读取证书 DER SIZE"; exit 1; }
[[ "$CERT_HASH" =~ ^[0-9a-fA-F]{64}$ ]] || { echo "[!] 无法读取证书 SHA-256"; exit 1; }

export KSU_MANAGER_PACKAGE="$PACKAGE"
export KSU_EXPECTED_SIZE="$CERT_SIZE"
export KSU_EXPECTED_HASH="${CERT_HASH,,}"

# Only build the requested KMI. This does not rebuild libkernelsu.so,
# libadbroot.so or libksud.so.
ODIR="$ROOT/out/$KMI"
mkdir -p "$ODIR"

command -v ddk >/dev/null 2>&1 || { echo "[!] 找不到 ddk"; exit 1; }
command -v llvm-strip >/dev/null 2>&1 || { echo "[!] 找不到 llvm-strip"; exit 1; }

cd "$ROOT/kernel"

# Preserve the DDK version marker if the local tree uses the same workaround
# as build-all.sh.
mv .ddk-version .ddk-version.bak 2>/dev/null || true
restore() { mv .ddk-version.bak .ddk-version 2>/dev/null || true; }
trap restore EXIT

echo "============================================"
echo " KernelSU dedicated Manager kernel build"
echo "============================================"
echo "Manager package : $KSU_MANAGER_PACKAGE"
echo "Manager APK     : $APK"
echo "Cert DER size   : $KSU_EXPECTED_SIZE"
echo "Cert SHA-256    : $KSU_EXPECTED_HASH"
echo "KMI             : $KMI"
echo

ddk build "$KMI" "ODIR=$ODIR" \
  "KSU_MANAGER_PACKAGE=$KSU_MANAGER_PACKAGE" \
  "KSU_EXPECTED_SIZE=$KSU_EXPECTED_SIZE" \
  "KSU_EXPECTED_HASH=$KSU_EXPECTED_HASH" \
  -e CONFIG_KSU=m

KO="$ODIR/kernelsu.ko"
[[ -f "$KO" ]] || { echo "[!] kernelsu.ko 未生成: $KO"; exit 1; }

OUT="$ROOT/kernel/kernelsu-${KMI}.ko"
cp -f "$KO" "$OUT"
llvm-strip -d "$OUT"

# Verify package/hash are present in the resulting module. The hash is normally
# visible as a string in apk_sign.o's compiled constants.
strings -a "$OUT" | grep -Fq "$PACKAGE" || {
  echo "[!] 生成的 kernelsu.ko 中未找到 $PACKAGE，停止。"; exit 1;
}
strings -a "$OUT" | grep -Fq "$KSU_EXPECTED_HASH" || {
  echo "[!] 生成的 kernelsu.ko 中未找到 EXPECTED_HASH，停止。"; exit 1;
}

echo
echo "========== SUCCESS =========="
echo "已生成专属 kernelsu.ko："
echo "  $OUT"
echo "package = $PACKAGE"
echo "size    = $KSU_EXPECTED_SIZE"
echo "hash    = $KSU_EXPECTED_HASH"
echo
echo "这里只重新编译 kernelsu.ko，不会重新编译："
echo "  libkernelsu.so"
echo "  libadbroot.so"
echo "  libksud.so"
