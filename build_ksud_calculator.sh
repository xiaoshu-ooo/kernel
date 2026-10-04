#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
KSUD_DIR="$ROOT/userspace/ksud"
TARGET_BASE="$ROOT/target"
OUT_ARM="$ROOT/manager/app/src/main/jniLibs/arm64-v8a/libksud.so"
OUT_X86="$ROOT/manager/app/src/main/jniLibs/x86_64/libksud.so"

PACKAGE_NAME="$(sed -n 's/^KSU_PACKAGE_NAME=//p' "$ROOT/manager/gradle.properties" | head -n1)"
PACKAGE_NAME="${PACKAGE_NAME:-com.coloros.calculator}"
export KSU_PACKAGE_NAME="$PACKAGE_NAME"

# Termux is running on Android/aarch64, so use Termux clang as the driver and
# the Android NDK sysroot only for headers/libs/builtins. This avoids pointing
# Cargo at an NDK linux-aarch64 clang that may not exist in the installed NDK.
NDK="${ANDROID_NDK_HOME:-${NDK_HOME:-}}"
if [ -z "$NDK" ] || [ ! -d "$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot" ]; then
    for d in \
        "$ROOT/../ndk/android-ndk-r27d" \
        "$HOME/ndk/android-ndk-r27d" \
        "$HOME/ndk/android-ndk-r29" \
        "$ANDROID_HOME/ndk/29.0.14206865"; do
        if [ -d "$d/toolchains/llvm/prebuilt/linux-x86_64/sysroot" ]; then NDK="$d"; break; fi
    done
fi
[ -n "$NDK" ] || { echo "[!] Android NDK not found"; exit 1; }
PRE="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
SYSROOT="$PRE/sysroot"
CLANG="${PREFIX:-/data/data/com.termux/files/usr}/bin/clang"
CLANGXX="${PREFIX:-/data/data/com.termux/files/usr}/bin/clang++"
[ -x "$CLANG" ] || { echo "[!] Termux clang not found: $CLANG"; exit 1; }

build_ksuinit() {
    local triple="$1" api="$2" arch="$3"
    local targetdir="$ROOT/userspace/ksud/bin/$arch"
    local builtins="$PRE/lib/clang/18/lib/linux/libclang_rt.builtins-${arch}-android.a"
    # NDK r27 clang resource version is usually 18; discover it if needed.
    if [ ! -f "$builtins" ]; then
        builtins="$(find "$PRE/lib/clang" -type f -name "libclang_rt.builtins-${arch}-android.a" 2>/dev/null | head -n1 || true)"
    fi
    [ -n "$builtins" ] && [ -f "$builtins" ] || { echo "[!] compiler builtins missing for $arch"; exit 1; }

    export "CC_${triple//-/_}"="$CLANG"
    export "CXX_${triple//-/_}"="$CLANGXX"
    export "CFLAGS_${triple//-/_}"="--target=$triple$api --sysroot=$SYSROOT"
    export "CXXFLAGS_${triple//-/_}"="--target=$triple$api --sysroot=$SYSROOT"
    export "CARGO_TARGET_${triple//-/_}_LINKER"="$CLANG"
    export RUSTFLAGS="-C linker=$CLANG -C target-feature=+crt-static -C link-arg=--target=$triple$api -C link-arg=--sysroot=$SYSROOT -C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-Wno-unused-command-line-argument -C link-arg=$builtins"

    echo "[+] Building ksuinit: $triple (API $api)"
    cargo build --release --package ksuinit --target="$triple"
    local src="$TARGET_BASE/$triple/release/ksuinit"
    [ -f "$src" ] || { echo "[!] ksuinit output missing: $src"; exit 1; }
    mkdir -p "$targetdir"
    cp -f "$src" "$targetdir/ksuinit"
    chmod 755 "$targetdir/ksuinit"
    echo "[+] Embedded asset: $targetdir/ksuinit"
}

echo "============================================"
echo " KernelSU Calculator ksud + ksuinit rebuild"
echo "============================================"
echo "ROOT        : $ROOT"
echo "KSU package : $KSU_PACKAGE_NAME"
echo "NDK         : $NDK"
echo

cd "$ROOT"

# Keep the RustEmbed KMI modules synchronized with the freshly compiled kernel
# modules. Otherwise ksud would embed an older Manager identity.
sync_kmi_modules() {
    local arch="$1"
    local srcdir="$ROOT/kernel"
    local dstdir="$KSUD_DIR/bin/$arch"
    mkdir -p "$dstdir"
    for kmi_file in "$srcdir"/kernelsu-android*.ko; do
        [ -f "$kmi_file" ] || continue
        local base kmi
        base="$(basename "$kmi_file")"
        kmi="${base#kernelsu-}"
        cp -f "$kmi_file" "$dstdir/${kmi}"
    done
}

# ksuinit is a required RustEmbed asset. Without it boot/init_boot patching
# fails exactly with: asset not found: ksuinit.
sync_kmi_modules "aarch64"
sync_kmi_modules "x86_64"
build_ksuinit "aarch64-linux-android" 26 "aarch64"
build_ksuinit "x86_64-linux-android" 26 "x86_64"

cd "$KSUD_DIR"
rm -rf target

build_ksud() {
    local triple="$1" arch="$2" api="$3" out="$4"
    export CARGO_TARGET_${triple//-/_}_LINKER="$CLANG"
    export CC_${triple//-/_}="$CLANG"
    export CXX_${triple//-/_}="$CLANGXX"
    export CFLAGS_${triple//-/_}="--target=$triple$api --sysroot=$SYSROOT"
    export CXXFLAGS_${triple//-/_}="--target=$triple$api --sysroot=$SYSROOT"
    export RUSTFLAGS="-C linker=$CLANG -C link-arg=--target=$triple$api -C link-arg=--sysroot=$SYSROOT"
    echo "[+] Building ksud: $triple"
    cargo build --release --package ksud --target="$triple"
    local src="$TARGET_BASE/$triple/release/ksud"
    [ -f "$src" ] || { echo "[!] ksud output missing: $src"; exit 1; }
    mkdir -p "$(dirname "$out")"
    cp -f "$src" "$out"
    chmod 755 "$out" || true
}

build_ksud "aarch64-linux-android" "aarch64" 26 "$OUT_ARM"
build_ksud "x86_64-linux-android" "x86_64" 26 "$OUT_X86"

echo
for f in "$OUT_ARM" "$OUT_X86"; do
    echo "===== $f ====="
    file "$f" || true
    echo "Embedded assets:"
    strings -a "$f" | grep -E '^(android(12|13|14|15|16|17)-(5\.10|5\.15|6\.1|6\.6|6\.12|6\.18)_kernelsu\.ko|ksuinit)$' | sort -u || true
done

echo
echo "[+] Done. Boot/init_boot patcher can now find ksuinit and the 8 KMI modules."
