# gradle-bintray-upload

English | [简体中文](README-CN.md)

> **Bintray and JCenter have been shut down, so the old `bintray.gradle` no longer works.**
> This repository now shows how to publish an Android library to **Maven Central** through the Central Portal. The repository name is kept so old links still resolve.
> The original (outdated, JCenter-era) article is still here for reference: [Android Studio: upload a library to JCenter and sync to Maven Central](http://blog.csdn.net/venshine/article/details/51742926).

## What this repository contains

| Path | Purpose |
|---|---|
| [`library/build.gradle`](library/build.gradle) | A minimal Android library with the publishing block you copy (see the bottom of the file) |
| [`build.gradle`](build.gradle) | Declares the publishing plugin version |
| [`gradle.properties`](gradle.properties) | Project metadata (`GROUP`, `VERSION_NAME`, `POM_*`) that ends up in the POM |
| [`gradle.properties.example`](gradle.properties.example) | Credential template for `~/.gradle/gradle.properties` (no real values) |

Verified with AGP 9.4.1 and Gradle 9.8.2 (see [Verified](#verified)).

## Why a plugin instead of a hand-written script

This repository now recommends the [`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) plugin (Apache-2.0) rather than a copy-and-paste `.gradle` script:

- Maven Central needs more than `maven-publish`: sources and javadoc jars, GPG signatures, `md5` / `sha1` checksums, a bundle in Maven layout, a Portal upload with a user token, and status polling. The plugin does all of it; a hand-written script would have to implement the bundle and upload part and track Portal API changes.
- The configuration is about ten lines, and coordinates and POM fields live in `gradle.properties`.
- It supports Android libraries with a single `release` variant. Latest release at the time of writing: 0.37.0 (2026-06-21), active repository (last push 2026-11-06), requires Gradle 9.0+, AGP 8.13+, JDK 17.
- The trade-off is one more third-party build dependency, and the author's release notes list AGP 9.2.1 / 9.3.0-rc01 as tested, so AGP 9.4.x relies on it simply working (it does in this repository's sample, but an actual upload has not been tried here).

If you prefer no plugin: use `maven-publish` + `signing` and Sonatype's [OSSRH Staging API service](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/). You must call its manual-upload endpoint after publishing, from the same IP.

## Security notes

- **Do not `apply from:` a remote script URL** (the old README recommended this). Anyone who can change that URL can run code on your build machine. Copy the few lines into your own project instead.
- Credentials and signing keys are read **only** from `~/.gradle/gradle.properties` or `ORG_GRADLE_PROJECT_*` environment variables, never from files inside the project. The old `local.properties` is no longer tracked and is git-ignored.

## Steps you must do yourself

These need your identity or secrets, so nobody can do them for you:

1. Register a Central Portal account and verify a namespace (step 1).
2. Generate the GPG key and publish the public key (step 2).
3. Generate the Portal user token (step 3).

## Tutorial

### 1. Register and verify a namespace

1. Sign up at <https://central.sonatype.com/>. Signing up with GitHub lets Sonatype usually verify `io.github.<your GitHub username>` automatically; only the account's own username counts, not a GitHub organization ([namespace docs](https://central.sonatype.org/register/namespace/)).
2. For your own domain (for example `com.example`), prove ownership with a DNS TXT record on `example.com`; for GitLab / Gitee / Bitbucket / GitHub organizations, create a temporary public repository named after the verification key shown in the Portal.
3. Your `GROUP` in `gradle.properties` must be inside a verified namespace. The old group `com.wx.android.common` would need proof that you own the domain `wx.com`, which is why the sample uses `io.github.venshine`.

### 2. Generate a GPG key and publish the public key

```bash
gpg --full-generate-key                 # RSA 3072, a name and email, a passphrase
gpg --list-secret-keys --keyid-format long
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

Central checks signatures against public keyservers; the docs name `keyserver.ubuntu.com`, `keys.openpgp.org` and `pgp.mit.edu` ([GPG docs](https://central.sonatype.org/publish/requirements/gpg/)). Sign with the primary key, not a signing sub key.

Export the private key for in-memory signing:

```bash
gpg --export-secret-keys --armor <KEY_ID> | awk '{printf "%s\\n", $0}'
```

### 3. Configure credentials

Generate a user token at <https://central.sonatype.com/usertoken> (it is shown once). Copy [`gradle.properties.example`](gradle.properties.example) to `~/.gradle/gradle.properties` and fill in the values:

```properties
mavenCentralUsername=<token-username>
mavenCentralPassword=<token-password>
signingInMemoryKey=<armored-private-key-on-one-line>
signingInMemoryKeyId=<last-8-hex-chars-of-key-id>
signingInMemoryKeyPassword=<key-passphrase>
```

On CI, use the same names as environment variables with the prefix `ORG_GRADLE_PROJECT_` (for example `ORG_GRADLE_PROJECT_mavenCentralUsername`) and store them as encrypted secrets.

### 4. Apply it to your library

Root `build.gradle`:

```groovy
plugins {
    id 'com.android.library' version '9.4.1' apply false
    id 'com.vanniktech.maven.publish' version '0.37.0' apply false
}
```

Library module `build.gradle`:

```groovy
plugins {
    id 'com.android.library'
    id 'com.vanniktech.maven.publish'
}

mavenPublishing {
    publishToMavenCentral()   // upload only; release in the Portal. Use publishToMavenCentral(true) for automatic release
    if (providers.gradleProperty('signingInMemoryKey').isPresent()) {
        signAllPublications()
    }
}
```

Copy the `GROUP`, `VERSION_NAME` and `POM_*` entries from [`gradle.properties`](gradle.properties) into your project's `gradle.properties` and replace them. `POM_NAME`, `POM_DESCRIPTION`, `POM_URL`, a license, a developer and the SCM entries are all required by Central ([requirements](https://central.sonatype.org/publish/requirements/)).

The plugin publishes the `release` variant with a sources jar and a javadoc jar automatically. Version must not end in `-SNAPSHOT` for Central.

### 5. Publish

```bash
./gradlew :library:publishToMavenLocal            # dry run: check the files in ~/.m2 (unsigned if no key is set)
./gradlew :library:publishToMavenCentral          # upload and wait for Portal validation
./gradlew :library:publishAndReleaseToMavenCentral  # upload and release automatically after validation
```

### 6. Release in the Portal, or release automatically

- With `publishToMavenCentral`, open <https://central.sonatype.com/publishing/deployments>, check the validated deployment and click **Publish**.
- With `publishAndReleaseToMavenCentral` (or `publishToMavenCentral(true)`), the deployment is published as soon as validation passes. Use it once your pipeline is trusted.

## Central at a glance

Checked against the official docs on 2026-11-09:

- OSSRH (`oss.sonatype.org`, `s01.oss.sonatype.org`) reached end of life on **2025-06-30**; the Central Publisher Portal replaces it ([announcement](https://central.sonatype.org/news/20250326_ossrh_sunset/)).
- Old OSSRH-style tooling can keep working through the [OSSRH Staging API service](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/) with a Portal user token.
- Every deployed file needs a `.asc` signature and `.md5` / `.sha1` checksums; non-pom projects need sources and javadoc jars ([requirements](https://central.sonatype.org/publish/requirements/)).
- The Portal API uploads a bundle with `POST /api/v1/publisher/upload`, authenticated by `Bearer base64(token-username:token-password)` ([Portal API](https://central.sonatype.org/publish/publish-portal-api/)).

## Verified

On 2026-11-09, on macOS with JDK 21, using a throwaway test key (deleted afterwards):

- `:library:testDebugUnitTest`, `:library:assembleRelease` pass.
- `:library:publishToMavenLocal` with an in-memory test key produces the `aar`, `pom`, `module`, sources jar, javadoc jar and a valid `.asc` for each; the POM contains name, description, url, license, developer and scm.
- Not tried: a real upload to the Portal, and the plugin's own bundle (checksums were checked on a bundle assembled by hand).

## License

Apache License 2.0, see [LICENSE](LICENSE).
