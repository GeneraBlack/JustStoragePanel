# Version Support Plan

This document tracks the active Just Storage Panel release lines across Minecraft 1.21.x and defines how new releases should be cut.

## Current Support Matrix

| Status | Minecraft | NeoForge | Branch | Version train | Notes |
| --- | --- | --- | --- | --- | --- |
| Maintenance baseline | 1.21.1 | 21.1.218 | release/1.21.1 | 1.0.x | Initial public line; hotfix-only if needed. |
| Released compatibility line | 1.21.5 | 21.5.97 | release/1.21.5 | 1.1.x | Public port line already validated and released. |
| Released compatibility line | 1.21.8 | 21.8.53 | release/1.21.8 | 1.2.x | Public port line already validated and released. |
| Current release line | 1.21.11 | 21.11.42 | release/1.21.11 | 1.3.x | Primary maintained 1.21.x line on ModDevGradle. |

## Current Recommendation

1. Cut new public 1.21.x releases from release/1.21.11.
2. Keep 1.21.1 as a fallback maintenance branch only for targeted hotfixes.
3. Patch 1.21.5 or 1.21.8 only if a real downstream pack or server still depends on them.
4. Do not backfill a dedicated 1.21.4 line unless an external compatibility requirement appears.
5. Reassess 1.22+ separately; do not mix that planning into the 1.21.11 release branch.

## Tooling Notes

- The 1.21.11 line uses ModDevGradle 2.0.141 instead of NeoGradle userdev.
- The current parchment keys are `parchment_minecraft_version` and `parchment_mappings_version`.
- JEI remains an optional dependency for the dedicated server and a client-side optional enhancement for users who want recipe transfer and GUI integration.
- Dedicated server startup is part of the validation baseline for 1.21.11 because the storage network is intentionally multiplayer-safe and server-driven.

## Tag And Release Naming

- GitHub release automation triggers on tags matching `v*`.
- Use tags that include both the mod version and the Minecraft line.
- Recommended tag examples:
  - v1.0.5-mc1.21.1
  - v1.1.0-mc1.21.5
  - v1.2.0-mc1.21.8
  - v1.3.0-mc1.21.11
- Keep `mod_version` as normal semver inside the branch.
- Let the tag carry the Minecraft suffix so GitHub releases stay unique across version lines.

## 1.21.11 Release Values

| Setting | Target value | Where |
| --- | --- | --- |
| Gradle wrapper | 9.2.1 | gradle/wrapper/gradle-wrapper.properties |
| ModDevGradle plugin | 2.0.141 | build.gradle |
| parchment_minecraft_version | 1.21.11 | gradle.properties |
| parchment_mappings_version | 2025.12.20 | gradle.properties |
| minecraft_version | 1.21.11 | gradle.properties |
| minecraft_version_range | [1.21.11] | gradle.properties |
| neo_version | 21.11.42 | gradle.properties |
| jei_version | 27.4.0.22 | gradle.properties |
| loader_version_range | [1,) | gradle.properties |
| mod_version for first 1.21.11 release | 1.3.0 | gradle.properties |

## 1.21.11 Validation Baseline

- `clean build --no-configuration-cache` completes successfully on JDK 21.
- `runClient` starts successfully with JEI present.
- `runServer` reaches the dedicated server ready state without a server-side classloading or payload registration failure.
- The storage network now uses NeoForge's transfer API directly instead of the old `IItemHandler` bridge.
- Multiplayer behavior remains server-driven and the dedicated server path is considered part of release readiness.

## Release Checklist Per Version Line

1. Update `minecraft_version`, `minecraft_version_range`, `neo_version`, dependency versions, and `mod_version` in gradle.properties.
2. Confirm build tooling is correct for the target line; for 1.21.11 this means ModDevGradle, not NeoGradle userdev.
3. Run a clean Gradle build for the branch.
4. Run `runClient` and confirm the mod and JEI initialize cleanly.
5. Run `runServer` whenever server-sensitive code changed or when preparing a public 1.21.11 release.
6. Update README and CHANGELOG for the exact Minecraft and NeoForge line being tagged.
7. Push a version tag using the recommended tag format.
8. Let release.yml publish the artifacts.
9. If Maven or CurseForge needs to be rerun, use republish.yml against the exact tag.

## Historical Notes

- 1.21.5 was the first post-1.21.1 compatibility line and introduced the required client item definition resources for the newer item model layout.
- 1.21.8 was the last release line before the NeoForge 21.9+ transfer and tooling changes, and it only required a relatively small network send adaptation.
- 1.21.11 is the first line in this repository that required both the ModDevGradle migration and the direct transfer-API cleanup in the storage network.