// Kotlin DSL 版本：与 library/build.gradle 等价，任选其一复制到自己的库模块。
plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.github.venshine.sample.kts"
    compileSdk = 36
    // 示例库是纯 Java，关闭内置 Kotlin，避免给使用方附带 kotlin-stdlib 依赖
    enableKotlin = false

    defaultConfig {
        minSdk = 21
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // 仅本示例需要：复用 library 模块的源码，只演示构建脚本写法；自己的库不要复制这段
    sourceSets {
        getByName("main").java.srcDir("../library/src/main/java")
        getByName("test").java.srcDir("../library/src/test/java")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

// ---- 发布到 Maven Central（复制这一段到自己的库模块即可）----
// 坐标和 POM 字段来自根目录 gradle.properties 的 GROUP / VERSION_NAME / POM_*，
// 本模块的 gradle.properties 只覆盖 artifactId 和名称
mavenPublishing {
    // 上传后到 Portal 页面手动点 Publish；要自动发布，改成 publishToMavenCentral(automaticRelease = true)
    publishToMavenCentral()
    // 只有配置了签名密钥才签名：本地 / CI 构建不需要密钥；
    // 发布时没带密钥，Portal 校验会因缺少 .asc 而拒绝，不会误发布未签名的版本
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
