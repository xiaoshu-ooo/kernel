# 8 KMI GitHub Actions 构建链

工作流：`.github/workflows/build-8kmi-ksud-manager.yml`

一次运行完成：

1. 调用原版 `build-lkm.yml` 矩阵构建 8 个 KMI。
2. 每个 KMI 同时构建 AArch64 和 x86_64，因此得到 16 个 `${KMI}_kernelsu.ko`。
3. 调用原版 `ksuinit.yml`。
4. 调用原版 `ksud.yml` 两个 Android target，并将全部 LKM 资产嵌入 ksud。
5. 将两个 ksud 可执行文件分别命名为 `libksud.so` 放入 `manager/app/src/main/jniLibs/arm64-v8a` 和 `x86_64`。
6. 使用 Java 21 + Android NDK r29 执行 `:app:assembleDebug`。
7. 校验最终 APK 中同时存在 `libksud.so`、`libkernelsu.so`、`libadbroot.so` 两个 ABI。

## 8 个 KMI

- android12-5.10
- android13-5.10
- android13-5.15
- android14-5.15
- android14-6.1
- android15-6.6
- android16-6.12
- android17-6.18

## 使用

将整个仓库上传 GitHub，进入 **Actions → Build 8 KMI KernelSU Calculator → Run workflow**。

注意：`.ko` 不在源码 ZIP 中预先伪造；它们由对应 KMI 的官方 DDK 容器在 Actions 中真实编译。
