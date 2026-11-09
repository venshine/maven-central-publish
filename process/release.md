# Release parameters (release-seal)

The generic steps live in the global `release-seal` skill. This file only records the parameters and exceptions for this project.

## What a release is

This repository is a tutorial plus a sample project. It publishes **no artifact** (not to Maven Central, not to JitPack). A "release" is only a Git tag marking a state of the tutorial. Never run `publishToMavenCentral` or `publishAndReleaseToMavenCentral` for the sample library.

## Version

- Semantic versioning `X.Y.Z` for the tag. Bump when the recommended plugin, the AGP / Gradle baseline or the tutorial steps change.
- `VERSION_NAME` in `gradle.properties` is only the sample's version and is not tied to the tag.
- Before tagging, check that the plugin version in `build.gradle`, `library/build.gradle` usage and the snippets in `README.md` / `README-CN.md` agree (`grep -rn "com.vanniktech.maven.publish" build.gradle README.md README-CN.md`).

## Pre-release checks

```bash
./gradlew :library:testDebugUnitTest :library:assembleRelease :library:publishToMavenLocal
```

Also re-check the facts the README quotes (OSSRH sunset, Portal API, requirements) against central.sonatype.org, and the plugin's latest release at https://github.com/vanniktech/gradle-maven-publish-plugin/releases.

## Tag

- The tag name is the bare version, without a `v` prefix: `1.0.0`
- Annotated tag with the message `gradle-bintray-upload X.Y.Z`, created on the release commit and pushed to `origin`

## GitHub Release

- Optional. If created: title is the version, notes in English, no attachments.

## Afterwards

- There is no product doc or ROADMAP; no other docs need syncing.
