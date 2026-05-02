# Changelog

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