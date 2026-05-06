# Changelog

## 1.3.0

- Ported the next public release line to Minecraft 1.21.11 and NeoForge 21.11.42.
- Migrated the development tooling for this line from NeoGradle userdev to ModDevGradle 2.0.141.
- Updated JEI integration to JEI 27.4.0.22 and ported payload identifiers from `ResourceLocation` to `Identifier`.
- Moved the storage network from the legacy `IItemHandler` bridge to NeoForge's transfer API using `ResourceHandler<ItemResource>`, `ItemUtil`, and transactional extraction.
- Validated the line with a clean Gradle build, a successful client start with JEI initialized, and a dedicated server startup through to the ready state.
- Kept the gameplay feature set unchanged; this release is the 1.21.11 compatibility and tooling-refresh line.

## 1.2.0

- Ported the next public release line to Minecraft 1.21.8 and NeoForge 21.8.53.
- Updated JEI integration to JEI 24.2.0.6 and adapted client-to-server payload sends to NeoForge's ClientPacketDistributor API.
- Validated the line with compileJava, processResources, a full Gradle build, and a successful 1.21.8 client start with JEI initialization.
- Kept the gameplay feature set unchanged; this release is the 1.21.8 compatibility line.

## 1.1.0

- Ported the public release line to Minecraft 1.21.5 and NeoForge 21.5.97.
- Updated JEI integration to JEI 21.4.0.27 and adapted Crafting Panel recipe transfer to the current 1.21.5 APIs.
- Updated block registration and item definition resources for the 1.21.5 registry and client item model changes.
- Validated the release line with a full Gradle build, client startup, JEI startup, and clean in-game smoke tests for panels and cables.
- Kept the gameplay feature set unchanged; this release is the 1.21.5 compatibility and polish line.

## 1.0.4

- Optimized network discovery with a cached topology per level and panel origin.
- Switched cache invalidation from whole-level clearing to position-targeted invalidation using a reverse index.
- Reduced passive panel refresh pressure and added configurable server refresh and client search debounce settings.
- Added multiplayer server impact documentation and updated README guidance for performance-sensitive setups.

## 1.0.3

- Published a clean follow-up release that includes the decoupled GitHub release workflow.
- Ensured Maven, CurseForge, and GitHub release steps can run independently during tagged releases.
- Kept the gameplay feature set unchanged for Minecraft 1.21.1 and NeoForge 21.1.218.

## 1.0.2

- Added the full CurseForge project description to the repository for release maintenance.
- Finalized the public 1.0.2 release metadata and documentation for GitHub, CurseForge, and Maven publication.
- Kept the gameplay feature set unchanged for Minecraft 1.21.1 and NeoForge 21.1.218.

## 1.0.1

- Repackaged the public release as version 1.0.1 for GitHub and CurseForge publication.
- Kept the same gameplay feature set for Minecraft 1.21.1 and NeoForge 21.1.218.

## 1.0.0

- Initial public release of Just Storage Panel for Minecraft 1.21.1 and NeoForge 21.1.218.
- Added the Access Panel, Crafting Panel, and Logic Cable.
- Added searchable and paged network item browsing with stack count overlays.
- Added JEI recipe transfer support and a JEI lookup button for the Crafting Panel.
- Added dedicated server compatibility and custom industrial block textures.