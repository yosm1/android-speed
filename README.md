# Laya Speed

独立安卓 Root 实验变速器。选择已运行的应用，通过 Frida Java 桥接同步设置 LayaAir 的 `Laya.timer.scale` 和 `Laya.systemTimer.scale`。当前版本面向 x86_64 MuMu，倍率 0.1～20，默认显示 5；连接后需手动应用倍率。

## 下载

在 GitHub Releases 页面下载 `LayaSpeed.apk`。需要已开启 Root 的 x86_64 安卓模拟器；当前并非通用安卓手机版本。

## 目录

- `android-app/app/src/main`：安卓界面、清单及本地注入资源。
- `android-app/build.ps1`：Java 编译、DEX 转换、打包及签名。
- `bridge-tools/app-agent.js`：安卓 APK 使用的注入脚本源码。

## 本机构建

构建工具仍保留在 `D:\exe\android-speed`，不随 Git 仓库提交。构建脚本当前使用已有 IntelliJ JBR；换电脑需修改工具路径。

```powershell
Set-Location D:\dev\code1\android-speed
npm.cmd ci --prefix .\bridge-tools --cache D:\exe\android-speed\npm-cache
& .\bridge-tools\node_modules\.bin\esbuild.cmd .\bridge-tools\app-agent.js --bundle --format=iife --platform=neutral --outfile=android-app/app/src/main/assets/agent.js
Copy-Item D:\exe\android-speed\frida-inject .\android-app\app\src\main\assets\frida-inject
& .\android-app\build.ps1
```

APK 输出到 `android-app/build/LayaSpeed.apk`。生成脚本、注入器二进制、构建目录及本地测试签名密钥均被 Git 忽略。已有测试密钥已复制到本地新目录，以保持升级安装签名一致；不要公开该密钥。该密钥仅用于本地测试，不适合正式发布。

## 使用和限制

正常打开目标游戏，再打开工具选择目标、连接、应用倍率。需要 Root 授权，不需要从工具启动游戏。仅 LayaAir 路线验证有效，选择其他应用不代表兼容。

恢复 1 倍后再结束使用。强制关闭工具不保证恢复；异常时重启目标游戏。高倍率可能影响共用计时器的逻辑、节点事件和结算。当前无悬浮窗和可靠后台服务，属于实验原型。

旧目录保留，未删除任何原始文件；游戏 APK、提取的第三方库、诊断结果及下载缓存未纳入源码仓库。
