# gradle-bintray-upload

[English](README.md) | 简体中文

> **Bintray 和 JCenter 已经关停，原来的 `bintray.gradle` 不能再用了。**
> 本仓库已改为演示如何通过 Central Portal 把 Android 库发布到 **Maven Central**。仓库名保持不变，旧链接仍然有效。
> 原来的（已过时，基于 JCenter）文章仍保留供参考：[Android Studio上传Library库到JCenter，并同步到Maven Central](http://blog.csdn.net/venshine/article/details/51742926)。

## 仓库内容

| 路径 | 作用 |
|---|---|
| [`library/build.gradle`](library/build.gradle) | 最小 Android 库示例，文件末尾是要复制的发布配置 |
| [`build.gradle`](build.gradle) | 声明发布插件版本 |
| [`gradle.properties`](gradle.properties) | 项目元数据（`GROUP`、`VERSION_NAME`、`POM_*`），会写进 POM |
| [`gradle.properties.example`](gradle.properties.example) | 放到 `~/.gradle/gradle.properties` 的凭据模板（不含真实值） |

已用 AGP 9.4.1、Gradle 9.8.2 验证（见[验证情况](#验证情况)）。

## 为什么用插件而不是自己写脚本

本仓库现在推荐 [`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) 插件（Apache-2.0），不再提供复制粘贴的 `.gradle` 脚本：

- Maven Central 需要的不只是 `maven-publish`：sources 和 javadoc jar、GPG 签名、`md5` / `sha1` 校验和、Maven 目录结构的 bundle、用 user token 上传到 Portal、轮询状态。插件全包了；自写脚本要自己实现 bundle 和上传，还要跟进 Portal API 变化。
- 配置约十行，坐标和 POM 字段放在 `gradle.properties`。
- 支持 Android 库的单个 `release` 变体。写作时最新版 0.37.0（2026-06-21），仓库活跃（最近一次推送 2026-11-06），要求 Gradle 9.0+、AGP 8.13+、JDK 17。
- 代价是多一个第三方构建依赖；作者的发布说明里测试过的 AGP 到 9.2.1 / 9.3.0-rc01，AGP 9.4.x 靠兼容性（本仓库示例构建和本地发布正常，但没有做过真实上传）。

不想用插件的话：`maven-publish` + `signing`，配合 Sonatype 的 [OSSRH Staging API 兼容服务](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/)，发布后必须从同一 IP 调用它的手动上传接口。

## 安全说明

- **不要再 `apply from:` 远程脚本 URL**（旧 README 推荐过）。谁能改动那个 URL，谁就能在你的构建机上执行任意代码。把几行配置复制到自己的项目里。
- 凭据和签名密钥**只**从 `~/.gradle/gradle.properties` 或 `ORG_GRADLE_PROJECT_*` 环境变量读取，不放在项目内的任何文件里。旧的 `local.properties` 已不再入库，并加入了忽略。

## 需要你本人操作的步骤

这几步涉及你的身份或密钥，别人无法代办：

1. 注册 Central Portal 账号并验证命名空间（第 1 步）。
2. 生成 GPG 密钥并上传公钥（第 2 步）。
3. 生成 Portal 的 user token（第 3 步）。

## 教程

### 1. 注册与命名空间

1. 到 <https://central.sonatype.com/> 注册。用 GitHub 登录注册时，Sonatype 通常能自动验证 `io.github.<你的 GitHub 用户名>`；只认账号自己的用户名，不含 GitHub 组织（[命名空间文档](https://central.sonatype.org/register/namespace/)）。
2. 自有域名（如 `com.example`）：在 `example.com` 加 DNS TXT 记录证明所有权；GitLab / Gitee / Bitbucket / GitHub 组织：按 Portal 给出的验证码新建同名临时公开仓库。
3. `gradle.properties` 里的 `GROUP` 必须在已验证的命名空间之内。旧的 `com.wx.android.common` 需要证明你拥有域名 `wx.com`，所以示例改用 `io.github.venshine`。

### 2. 生成 GPG 密钥并上传公钥

```bash
gpg --full-generate-key                 # RSA 3072，填姓名、邮箱、口令
gpg --list-secret-keys --keyid-format long
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

Central 会到公共密钥服务器校验签名，文档列出的是 `keyserver.ubuntu.com`、`keys.openpgp.org`、`pgp.mit.edu`（[GPG 文档](https://central.sonatype.org/publish/requirements/gpg/)）。要用主密钥签名，不要用签名子密钥。

导出私钥供内存签名使用：

```bash
gpg --export-secret-keys --armor <KEY_ID> | awk '{printf "%s\\n", $0}'
```

### 3. 配置凭据

到 <https://central.sonatype.com/usertoken> 生成 user token（只显示一次）。把 [`gradle.properties.example`](gradle.properties.example) 复制为 `~/.gradle/gradle.properties` 并填值：

```properties
mavenCentralUsername=<token-username>
mavenCentralPassword=<token-password>
signingInMemoryKey=<armored-private-key-on-one-line>
signingInMemoryKeyId=<last-8-hex-chars-of-key-id>
signingInMemoryKeyPassword=<key-passphrase>
```

CI 里用同名环境变量，加前缀 `ORG_GRADLE_PROJECT_`（如 `ORG_GRADLE_PROJECT_mavenCentralUsername`），存成加密 secret。

### 4. 应用到自己的库

根目录 `build.gradle`：

```groovy
plugins {
    id 'com.android.library' version '9.4.1' apply false
    id 'com.vanniktech.maven.publish' version '0.37.0' apply false
}
```

库模块 `build.gradle`：

```groovy
plugins {
    id 'com.android.library'
    id 'com.vanniktech.maven.publish'
}

mavenPublishing {
    publishToMavenCentral()   // 只上传，到 Portal 手动发布；要自动发布改成 publishToMavenCentral(true)
    if (providers.gradleProperty('signingInMemoryKey').isPresent()) {
        signAllPublications()
    }
}
```

把 [`gradle.properties`](gradle.properties) 里的 `GROUP`、`VERSION_NAME`、`POM_*` 复制到自己项目的 `gradle.properties` 并替换。`POM_NAME`、`POM_DESCRIPTION`、`POM_URL`、许可证、开发者和 SCM 都是 Central 的必填项（[要求](https://central.sonatype.org/publish/requirements/)）。

插件会自动发布 `release` 变体，并带上 sources 和 javadoc jar。发到 Central 的版本号不能以 `-SNAPSHOT` 结尾。

### 5. 发布

```bash
./gradlew :library:publishToMavenLocal            # 预演：到 ~/.m2 检查产物（没配密钥则不签名）
./gradlew :library:publishToMavenCentral          # 上传并等待 Portal 校验
./gradlew :library:publishAndReleaseToMavenCentral  # 上传，校验通过后自动发布
```

### 6. 在 Portal 发布 / 自动发布

- 用 `publishToMavenCentral` 时，打开 <https://central.sonatype.com/publishing/deployments>，确认校验通过的部署后点 **Publish**。
- 用 `publishAndReleaseToMavenCentral`（或 `publishToMavenCentral(true)`）时，校验一通过就直接发布。流水线可信之后再用。

## Central 现状速览

2026-11-09 对照官方文档核对：

- OSSRH（`oss.sonatype.org`、`s01.oss.sonatype.org`）已于 **2025-06-30** 终止服务，由 Central Publisher Portal 取代（[公告](https://central.sonatype.org/news/20250326_ossrh_sunset/)）。
- 旧的 OSSRH 工具链可以通过 [OSSRH Staging API 兼容服务](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/)配合 Portal user token 继续使用。
- 每个发布文件都要有 `.asc` 签名和 `.md5` / `.sha1` 校验和；非 pom 项目要有 sources 和 javadoc jar（[要求](https://central.sonatype.org/publish/requirements/)）。
- Portal API 用 `POST /api/v1/publisher/upload` 上传 bundle，鉴权为 `Bearer base64(token用户名:token密码)`（[Portal API](https://central.sonatype.org/publish/publish-portal-api/)）。

## 验证情况

2026-11-09，macOS、JDK 21，使用临时生成的测试密钥（用后已删除）：

- `:library:testDebugUnitTest`、`:library:assembleRelease` 通过。
- 带内存测试密钥的 `:library:publishToMavenLocal` 产出 `aar`、`pom`、`module`、sources jar、javadoc jar，且各自带有效的 `.asc`；POM 含 name、description、url、license、developer、scm。
- 未做：真实上传到 Portal；插件自己生成的 bundle（校验和是在手工拼装的 bundle 上检查的）。

## 许可证

Apache License 2.0，见 [LICENSE](LICENSE)。
