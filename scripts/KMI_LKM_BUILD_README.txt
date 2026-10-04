KernelSU 8-KMI LKM asset chain

Supported KMI:
  android12-5.10
  android13-5.10
  android13-5.15
  android14-5.15
  android14-6.1
  android15-6.6
  android16-6.12
  android17-6.18

Official source chain:
  .github/workflows/build-lkm.yml
    -> .github/workflows/ddk-lkm.yml
    -> builds one arm64 and one x86_64 kernelsu.ko per KMI
  .github/workflows/ksud.yml
    -> downloads all KMI LKM artifacts
    -> copies them to userspace/ksud/bin/aarch64 and x86_64
    -> RustEmbed embeds them into ksud
  userspace/ksud/src/boot_patch.rs
    -> detects current KMI
    -> loads <kmi>_kernelsu.ko

Local build:
  1. Use a DDK environment containing /opt/ddk and the KMI source/toolchains.
  2. Run kernel/build-all.sh for aarch64.
  3. Prepare x86_64 KDIRs with scripts/prepare-ddk-x64.sh, then run kernel/build-all-x64.sh.
  4. Put outputs under out-kmi-lkm/aarch64 and out-kmi-lkm/x86_64.
  5. Run scripts/pack-kmi-assets.sh.
  6. Run scripts/check-kmi-assets.sh.
  7. Build ksud/Manager.

Important:
  libkernelsu.so is the Manager JNI library and is NOT a replacement for any *_kernelsu.ko.
  The source tree intentionally does not contain generated .ko binaries until the LKM build step runs.
