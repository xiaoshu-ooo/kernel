#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-/data/data/com.termux/files/usr/opt/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"

PACKAGE_NAME="com.coloros.calculator"
MANAGER_DIR="$ROOT/manager"
APK_SCRIPT="$MANAGER_DIR/apk#U7f16#U8bd1.sh"
APK="$MANAGER_DIR/app/build/outputs/apk/debug/app-debug.apk"
CERT_TOOL="$ROOT/scripts/apk_v2_cert.py"
KMI="${KSU_KMI:-android15-6.6}"
INPUT_BOOT="${1:-}"
OUTPUT_BOOT="${2:-}"

if [[ -z "$INPUT_BOOT" ]]; then
  echo "用法: $0 <原始boot或init_boot.img> [输出.img]"
  echo "默认 KMI: $KMI（可用 KSU_KMI=android15-6.6 修改）"
  exit 2
fi
INPUT_BOOT="$(realpath "$INPUT_BOOT")"
[[ -f "$INPUT_BOOT" ]] || { echo "[!] 镜像不存在: $INPUT_BOOT"; exit 1; }
if [[ -z "$OUTPUT_BOOT" ]]; then
  OUTPUT_BOOT="$ROOT/kernelsu_patched_$(date +%Y%m%d_%H%M%S).img"
else
  OUTPUT_BOOT="$(realpath -m "$OUTPUT_BOOT")"
fi

export KSU_PACKAGE_NAME="$PACKAGE_NAME"
export KSU_MANAGER_PACKAGE="$PACKAGE_NAME"

unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS

echo "============================================"
echo " KernelSU Calculator dedicated Manager build"
echo "============================================"
echo "Package : $PACKAGE_NAME"
echo "KMI     : $KMI"
echo "Input   : $INPUT_BOOT"
echo "Output  : $OUTPUT_BOOT"

# 1) Build the Manager once. The debug certificate is stable across rebuilds
# as long as the signing configuration is unchanged. This gives the kernel the
# exact DER certificate length/hash that apk_sign.c checks.
echo
echo "[1/6] Building Manager APK to obtain signer certificate..."
cd "$MANAGER_DIR"
bash "$APK_SCRIPT"
[[ -f "$APK" ]] || { echo "[!] APK missing: $APK"; exit 1; }
cd "$ROOT"

readarray -t CERT < <(python3 "$CERT_TOOL" "$APK")
CERT_SIZE="${CERT[0]#SIZE=}"
CERT_HASH="${CERT[1]#SHA256=}"
[[ "$CERT_SIZE" =~ ^[0-9]+$ && "$CERT_HASH" =~ ^[0-9a-f]{64}$ ]] || {
  echo "[!] Failed to parse APK certificate"; exit 1;
}

echo "[+] Manager certificate size : $CERT_SIZE"
echo "[+] Manager certificate hash : $CERT_HASH"

# 2) Rebuild every KMI module with BOTH package and certificate binding.
echo
echo "[2/6] Building KernelSU modules with dedicated Manager binding..."
export KSU_EXPECTED_SIZE="$CERT_SIZE"
export KSU_EXPECTED_HASH="$CERT_HASH"
export KSU_KMIS="${KSU_KMIS:-android12-5.10 android13-5.10 android13-5.15 android14-5.15 android14-6.1 android15-6.6 android16-6.12 android17-6.18}"
bash "$ROOT/kernel/build-all.sh"

# 3) Rebuild ksud/ksuinit so its embedded KMI modules are the newly bound ones.
echo
echo "[3/6] Rebuilding ksud + ksuinit with the bound KMI modules..."
bash "$ROOT/build_ksud_calculator.sh"

# 4) Rebuild the final Manager APK using the updated libksud.so.
echo
echo "[4/6] Rebuilding final Manager APK..."
cd "$MANAGER_DIR"
bash "$APK_SCRIPT"
cd "$ROOT"
[[ -f "$APK" ]] || { echo "[!] Final APK missing"; exit 1; }
readarray -t CERT2 < <(python3 "$CERT_TOOL" "$APK")
[[ "${CERT2[0]#SIZE=}" == "$CERT_SIZE" && "${CERT2[1]#SHA256=}" == "$CERT_HASH" ]] || {
  echo "[!] Manager signer changed during final build; refusing to patch boot."
  exit 1
}

echo "[OK] Final Manager signer matches kernel configuration."

# 5) Patch the supplied boot/init_boot image with the exact KMI module.
echo
echo "[5/6] Patching boot/init_boot..."
KSUD="$MANAGER_DIR/app/src/main/jniLibs/arm64-v8a/libksud.so"
[[ -f "$KSUD" ]] || { echo "[!] ARM64 libksud.so missing: $KSUD"; exit 1; }
chmod +x "$KSUD" || true
"$KSUD" boot-patch -b "$INPUT_BOOT" --kmi "$KMI" --module "$ROOT/kernel/kernelsu-${KMI}.ko" -o "$ROOT/.ksu-patched-work" --out-name "$(basename "$OUTPUT_BOOT")"
PATCHED="$ROOT/.ksu-patched-work/$(basename "$OUTPUT_BOOT")"
[[ -f "$PATCHED" ]] || { echo "[!] Patcher did not create: $PATCHED"; exit 1; }
mv -f "$PATCHED" "$OUTPUT_BOOT"
rmdir "$ROOT/.ksu-patched-work" 2>/dev/null || true

# 6) Final consistency checks.
echo
echo "[6/6] Final verification..."
KO="$ROOT/kernel/kernelsu-${KMI}.ko"
echo "Manager package : $PACKAGE_NAME"
echo "Cert size       : $CERT_SIZE"
echo "Cert SHA-256    : $CERT_HASH"
echo "Kernel module   : $KO"
echo "Patched image   : $OUTPUT_BOOT"
strings -a "$KO" | grep -Fq "$PACKAGE_NAME" && echo "[OK] com.coloros.calculator is embedded in kernelsu.ko" || {
  echo "[!] Package string not visible in stripped module; verify build log/Kbuild."; exit 1;
}
ls -lh "$APK" "$KO" "$OUTPUT_BOOT"
echo
echo "========== SUCCESS =========="
echo "这个输出镜像的 kernelsu.ko 已绑定："
echo "  package = $PACKAGE_NAME"
echo "  cert    = $CERT_HASH"
echo "并已使用 KMI = $KMI 自动 patch。"
