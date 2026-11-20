# Android 实验环境配置

| 配置 | 版本 |
| --- | --- |
| Gradle 项目名 | `AircraftWar2026` |
| Gradle 运行 JDK | 21 |
| 应用 Java 源码和字节码级别 | 17 |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| 编译与目标 SDK | API 36 |
| 最低 SDK | API 30 |
| Android SDK Build-Tools | 36.0.0 |

## 在 Android Studio 中运行

用 Android Studio 打开 `AndroidLab` 根目录。Gradle 同步时会按 `gradle/gradle-daemon-jvm.properties` 查找本机的 JDK 21。此次使用的 JDK 位于 `C:\Users\Lenovo\.jdks\ms-21.0.10`。

本机的 SDK 位于 `D:\Android SDK`，由本地文件 `local.properties` 指定。其他电脑用 SDK Manager 安装 Android 16（API 36）、Build-Tools 36.0.0 和 Platform-Tools，再同步工程。同步完成后，选择 `app` 和一个 API 30 或以上的设备，点击 **Run**。当前骨架启动后显示「Android Lab 1 环境骨架」。指导书指定的 IDE 系列为 Android Studio Panda。

## 从 PowerShell 构建

本机终端默认 Java 是 8。在项目根目录执行下列命令，将 `JAVA_HOME` 临时指向 JDK 21。其他电脑按实际安装路径调整第一行。

```powershell
$env:JAVA_HOME = 'C:\Users\Lenovo\.jdks\ms-21.0.10'
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

构建输出位于 `app/build/outputs/apk/debug/app-debug.apk`。
