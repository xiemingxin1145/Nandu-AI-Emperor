# 《南渡无悔》Android 更新与签名硬规则

任何 AI、开发者或自动化在制作新的可安装 APK 前都必须先检查本文件。

## 1. 版本号必须递增

- 每一个准备交给手机安装的新测试/正式 APK，都必须提升 `app/build.gradle.kts` 的 `versionCode`。
- 对玩家可见的更新同步提升 `versionName`。补丁按 `V1.6.1 -> V1.6.2 -> V1.6.3`；较大里程碑再升 `V1.7.0`。
- 禁止用旧 `versionCode` 覆盖新 APK，否则 Android 可能拒绝升级或造成版本识别混乱。

## 2. Android “更新”必须保持同一签名

Android 升级安装要求新旧 APK 的 applicationId 和签名证书一致。**签名不能每次换。**

- **V1.6.1 起**，Debug/RC 测试包固定使用 `tools/signing/nandu-dev-debug.keystore`，以 V1.6.1 为长期测试签名基线。
- V1.6.0 RC2 使用的是一次性的手工测试证书，后续复核发现其私钥未保留；因此 RC2 -> V1.6.1 可能需要最后一次卸载重装。V1.6.1 之后禁止再换测试签名。
- 稳定测试证书 SHA-256 指纹：
  `EC:1B:19:D8:FF:E5:19:83:A1:B3:3B:BE:DC:77:24:D9:D4:41:21:8B:7E:DD:6C:6E:20:62:CF:97:D3:84:38:75`
- 该密钥只为公开仓库中的开发测试使用，绝不作为未来 Play Store/生产正式签名。
- 未来正式发行改用私有 release keystore/GitHub Secrets，并从首次正式发行起永久保持同一生产签名。

## 3. Release 分支的 CI 必须拦截旧版本

`release/**` 分支构建前运行 `scripts/check_android_release_policy.sh`。若版本号没有高于 main、稳定测试签名缺失或 Gradle 未引用固定测试签名，构建直接失败，并明确提示先修正版本/签名。

CI 构建后必须再用 `apksigner verify --print-certs` 校验证书指纹；若不是上述固定指纹，APK 不得交付。

## 4. 每次交付 APK 前的检查顺序

1. 提升 `versionCode` / `versionName`；
2. 确认签名策略没有变化；
3. 跑 unit tests；
4. 构建 APK；
5. 用 `apksigner verify --print-certs` 核对证书；
6. 拆包确认本次新增图片、视频、BGM、旁白、环境声真的进入 APK；
7. 再交给手机安装验收。

> 重要：以后任何 AI 收到“升级南渡 APK / 打个新包 / 做下一版”的任务，都应先执行这里的版本与签名检查，不要复用旧版本号，也不要临时生成新 debug keystore。V1.6.1 是固定测试签名基线，后续测试版必须与它保持一致。
