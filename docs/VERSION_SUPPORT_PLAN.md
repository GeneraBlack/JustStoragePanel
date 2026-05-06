# Version Support Plan

This document defines which Minecraft and NeoForge lines Just Storage Panel should target next and how releases should be cut for each line.

## Target Lines

| Status | Minecraft | NeoForge | Purpose | Notes |
| --- | --- | --- | --- | --- |
| Current maintenance | 1.21.1 | 21.1.x | Stable baseline | Current public release line. |
| Release-ready | 1.21.5 | 21.5.x | Primary expansion target | Build, client, JEI, and in-game validation completed. |
| Planned follow-up | 1.21.8 | 21.8.x | Late 1.21.x coverage | Good additional reach without jumping into the 21.9 transfer rework immediately. |
| Deferred | 1.21.11 | 21.11.x | Revisit after API migration | NeoForge 21.9+ transfer API changes should be handled first. |

## Versions To Skip For Now

- Do not create a dedicated 1.21.4 line unless a modpack or server partner explicitly asks for it.
- Do not jump straight from 1.21.1 to 1.21.11 until the inventory transfer layer is migrated away from the old IItemHandler-centric assumptions.

## Release Order

1. Keep 1.21.1 as the maintenance line for hotfixes and compatibility fixes.
2. Port and release 1.21.5 as the next actively promoted line.
3. Port and release 1.21.8 after the 1.21.5 line is stable.
4. Reassess 1.21.11 only after the NeoForge 21.9+ transfer rework has been planned and implemented.

## Branch Strategy

- Use one long-lived branch per Minecraft line.
- Recommended branch names:
  - release/1.21.1
  - release/1.21.5
  - release/1.21.8
- Keep the newest supported line as the main development branch once it becomes the primary target.

## Tag And Release Naming

- The GitHub release workflow already triggers on tags matching v*.
- Use tags that include both the mod version and the Minecraft line.
- Recommended tag format:
  - v1.0.5-mc1.21.1
  - v1.1.0-mc1.21.5
  - v1.2.0-mc1.21.8
- Keep mod_version as normal semver inside the branch.
- Let the tag carry the Minecraft suffix so GitHub releases stay unique across version lines.

## Version Trains

| Minecraft line | Branch | Mod version train | First planned tag |
| --- | --- | --- | --- |
| 1.21.1 | release/1.21.1 | 1.0.x | v1.0.5-mc1.21.1 |
| 1.21.5 | release/1.21.5 | 1.1.x | v1.1.0-mc1.21.5 |
| 1.21.8 | release/1.21.8 | 1.2.x | v1.2.0-mc1.21.8 |

## First Concrete Release Line: 1.21.5

### Target build values

| Setting | Target value | Where |
| --- | --- | --- |
| Gradle wrapper | 9.2.1 | gradle/wrapper/gradle-wrapper.properties |
| userdev plugin | 7.1.25 | build.gradle |
| neogradle.subsystems.parchment.minecraftVersion | 1.21.5 | gradle.properties |
| neogradle.subsystems.parchment.mappingsVersion | 2025.06.15 | gradle.properties |
| minecraft_version | 1.21.5 | gradle.properties |
| minecraft_version_range | [1.21.5] | gradle.properties |
| neo_version | 21.5.97 | gradle.properties |
| jei_version | 21.4.0.27 | gradle.properties |
| loader_version_range | [1,) | gradle.properties |
| mod_version for first release | 1.1.0 | gradle.properties |

### Branch plan

1. Merge any remaining 1.21.1-only fixes first.
2. Create release/1.21.5 from the current main branch.
3. Perform the port on release/1.21.5 until build.yml is green.
4. Tag the first release on that branch as v1.1.0-mc1.21.5.
5. Keep release/1.21.1 open for hotfixes as the 1.0.x line.
6. After 1.21.5 is stable, use it as the base for the later 1.21.8 line.

### Readiness status

- The 1.21.5 dependency line is now active in gradle.properties with mod_version 1.1.0.
- compileJava, processResources, and the full Gradle build complete successfully on JDK 21.
- runClient reaches a playable integrated world with JEI fully initialized.
- In-game smoke tests for Access Panel, Crafting Panel, Logic Cable, and JEI transfer were completed successfully.
- The new assets/juststoragepanel/items definitions required for 1.21.5 are present.

### Porting checklist for this repo

1. Update the 1.21.5 property values listed above.
2. Adjust block API call sites that still assume the old 1.21.1 signatures, especially LogicCableBlock.updateShape.
3. Add client item definition JSON files under assets/juststoragepanel/items/ for access_panel, crafting_panel, and logic_cable. The repo currently only contains legacy models/item JSONs.
4. Re-run the full build and confirm the client, server, and data runs still initialize cleanly.
5. Update README requirements from 1.21.1 / 21.1.218 to the 1.21.5 line.
6. Add a CHANGELOG entry for the new line and note whether gameplay changed or the release is a compatibility port.

### Release steps for 1.21.5

1. Push release/1.21.5 and let build.yml validate the branch.
2. Confirm build artifacts are named with the 1.21.5 artifact suffix.
3. Create and push the tag v1.1.0-mc1.21.5.
4. Let release.yml create the GitHub release and publish Maven and CurseForge artifacts.
5. Verify CurseForge with the game version filter set to 1.21.5 and the loader filter set to NeoForge.
6. If one publishing target fails while the tag is otherwise correct, rerun publication with republish.yml against v1.1.0-mc1.21.5.

## Release Checklist Per Version Line

1. Update minecraft_version, minecraft_version_range, neo_version, and dependency versions in gradle.properties.
2. Adjust code for the target line's API changes.
3. Run the full Gradle build for that branch.
4. Update README and CHANGELOG if player-facing behavior or requirements changed.
5. Push a version tag using the recommended tag format.
6. Let release.yml publish the artifacts.
7. If Maven or CurseForge needs to be rerun, use republish.yml against the exact tag.

## Practical Recommendation

- Support three lines in this order: 1.21.1, 1.21.5, and 1.21.8.
- Treat 1.21.1 as maintenance-only after 1.21.5 ships.
- Treat 1.21.8 as the last 1.21.x line before the larger 21.9+ migration work.