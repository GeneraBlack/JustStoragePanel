# Version Support Plan

This document tracks the active Just Storage Panel release lines across Minecraft 1.21.x and 26.x and defines how new releases should be cut.

## Current Support Matrix

| Status | Minecraft | NeoForge | Branch | Version train | Notes |
| --- | --- | --- | --- | --- | --- |
| Maintenance baseline | 1.21.1 | 21.1.218 | release/1.21.1 | 1.0.x | Initial public line; hotfix-only if needed. |
| Released compatibility line | 1.21.5 | 21.5.97 | release/1.21.5 | 1.1.x | Public port line already validated and released. |
| Released compatibility line | 1.21.8 | 21.8.53 | release/1.21.8 | 1.2.x | Public port line already validated and released. |
| Released compatibility line | 1.21.11 | 21.11.42 | release/1.21.11 | 1.3.x | Last stable 1.21.x line on ModDevGradle. |
| Released compatibility line | 26.1.2 | 26.1.2.41-beta | release/26.1.2 | 1.4.x | First 26.x release line; Java 25 and 26.1 GUI/input APIs. |
| Current release line | 26.2 | 26.2.0.79 | release/26.2 | 1.5.x | Primary maintained line for Minecraft 26.2. |

## Current Recommendation

1. Cut new public releases from release/26.2.
2. Keep 26.2 as the primary maintained line for current public releases.
3. Keep 26.1.2 and 1.21.11 only for targeted hotfixes when a downstream pack or server cannot move yet.
4. Keep 1.21.1 as the long-tail fallback branch; only revisit 1.21.5 or 1.21.8 if a concrete compatibility demand appears.

## Tooling Notes

- The 26.2 line uses ModDevGradle 2.0.141.
- Java 25 is required locally and in CI for this line.
- Parchment is not used for 26.2.
- JEI remains an optional dependency for the dedicated server and a client-side optional enhancement for users who want recipe transfer and GUI integration.
- Dedicated server startup is part of the validation baseline for 26.2 because the storage network is intentionally multiplayer-safe and server-driven.

## Tag And Release Naming

- GitHub release automation triggers on tags matching `v*`.
- Use tags that include both the mod version and the Minecraft line.
- Recommended tag examples:
  - v1.0.5-mc1.21.1
  - v1.1.0-mc1.21.5
  - v1.2.0-mc1.21.8
  - v1.3.0-mc1.21.11
  - v1.4.0-mc26.1.2
  - v1.5.0-mc26.2
- Keep `mod_version` as normal semver inside the branch.
- Let the tag carry the Minecraft suffix so GitHub releases stay unique across version lines.

## 26.2 Release Values

| Setting | Target value | Where |
| --- | --- | --- |
| Gradle wrapper | 9.2.1 | gradle/wrapper/gradle-wrapper.properties |
| ModDevGradle plugin | 2.0.141 | build.gradle |
| java.toolchain.languageVersion | 25 | build.gradle |
| minecraft_version | 26.2 | gradle.properties |
| minecraft_version_range | [26.2] | gradle.properties |
| neo_version | 26.2.0.79 | gradle.properties |
| jei_version | 30.31.0.206 | gradle.properties |
| loader_version_range | [1,) | gradle.properties |
| mod_version for first 26.2 release | 1.5.0 | gradle.properties |

## 26.2 Validation Baseline

- `compileJava processResources --no-configuration-cache` completes successfully on JDK 25.
- `build` completes successfully and produces valid release artifacts.
- `runGameTestServer` completes successfully with all game tests passing.
- `runData` completes successfully.
- `runServer` reaches the dedicated server ready state without a server-side classloading or payload registration failure.
- Multiplayer behavior remains server-driven and the dedicated server path is considered part of release readiness.

## Release Checklist Per Version Line

1. Update `minecraft_version`, `minecraft_version_range`, `neo_version`, dependency versions, and `mod_version` in gradle.properties.
2. Confirm build tooling is correct for the target line; for 26.2 this means ModDevGradle, Java 25, and no Parchment block.
3. Run a clean Gradle build or at least `compileJava processResources --no-configuration-cache` for the branch.
4. Run `runClient` and confirm the mod and JEI initialize cleanly.
5. Run `runServer` whenever server-sensitive code changed or when preparing a public 26.2 release.
6. Update README and CHANGELOG for the exact Minecraft and NeoForge line being tagged.
7. Ensure the release branch exists and push a version tag using the recommended tag format.
8. Let release.yml publish the artifacts.
9. If Maven or CurseForge needs to be rerun, use republish.yml against the exact tag.

## Historical Notes

- 1.21.5 was the first post-1.21.1 compatibility line and introduced the required client item definition resources for the newer item model layout.
- 1.21.8 was the last release line before the NeoForge 21.9+ transfer and tooling changes, and it only required a relatively small network send adaptation.
- 1.21.11 is the first line in this repository that required both the ModDevGradle migration and the direct transfer-API cleanup in the storage network.
- 26.1.2 is the first line that required Java 25, the `GuiGraphicsExtractor` screen flow, `ContainerInput` menu clicks, and the new `BreakBlockEvent` server invalidation hook.
- 26.2 is the first line on Minecraft 26.2, bringing JEI 30 compatibility and verified server/GameTest stability.