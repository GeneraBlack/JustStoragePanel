Just Storage Panel
==================

Just Storage Panel is a focused NeoForge mod for Minecraft 1.21.1 that adds a compact item network built around exactly three blocks.

Features
--------

- Access Panel: browse the connected network, insert items, and extract items.
- Crafting Panel: adds a 3x3 crafting grid on top of the Access Panel workflow.
- Logic Cable: connects panels to inventories and compatible controller-style blocks.

Network Model
-------------

The network starts at a panel and scans through adjacent Logic Cables and other panels. Any neighboring block that exposes a compatible item handler is treated as a storage endpoint.

The mod also includes targeted runtime heuristics for Sophisticated Storage controller-style access blocks such as controller, storage IO, storage input, and storage output, which improves attachment reliability for panels and cables.

JEI Integration
---------------

- Recipe transfer into the Crafting Panel is supported.
- A dedicated JEI button is available in the Crafting Panel GUI when JEI is installed on the client.
- JEI remains optional and client-side only.

Requirements
------------

- Minecraft 1.21.1
- NeoForge 21.1.218
- Java 21

Building
--------

Build the mod locally with:

```powershell
.\gradlew.bat build
```

The generated artifact is written to build/libs and is named like this:

```text
juststoragepanel-1.21.1-1.0.0.jar
```

Publishing
----------

For a local Maven-style publication, run:

```powershell
.\gradlew.bat publish
```

Without any extra configuration this publishes to the local `repo` folder.

To publish to a remote Maven repository instead, provide these environment variables or Gradle properties:

- `MAVEN_URL` or `mavenUrl`
- `MAVEN_USERNAME` or `mavenUsername`
- `MAVEN_PASSWORD` or `mavenPassword`

The published Maven coordinates use this artifact id:

```text
de.juststoragepanel:juststoragepanel-1.21.1:1.0.0
```

To publish the built jar to CurseForge, run:

```powershell
.\gradlew.bat --no-configuration-cache publishCurseForge
```

Required configuration for CurseForge:

- `CURSEFORGE_PROJECT_ID` or `curseforgeProjectId`
- `CURSEFORGE_TOKEN` or `curseforgeToken`

Optional CurseForge configuration:

- `CURSEFORGE_RELEASE_TYPE` or `curseforgeReleaseType` with `release`, `beta`, or `alpha`

The default changelog source for CurseForge releases is `CHANGELOG.md`.

The CurseForge Gradle plugin currently does not support Gradle's configuration cache cleanly, so the command above explicitly disables it for that publish step.

GitHub Workflows
----------------

- `build.yml` builds the project on pushes and pull requests.
- `release.yml` builds tagged releases, uploads the jar as a GitHub release asset, and can also publish to Maven and CurseForge when the required repository variables and secrets are configured.

Recommended release flow:

1. Create your own Git repository and point `origin` to it.
2. Push the main branch.
3. Create and push a version tag such as `v1.0.0`.
4. Configure `MAVEN_URL`, `MAVEN_PASSWORD`, `CURSEFORGE_PROJECT_ID`, and `CURSEFORGE_TOKEN` in GitHub if you want automated publishing.

Development Run
---------------

Start the client development environment with:

```powershell
.\gradlew.bat runClient
```

Start the dedicated development server with:

```powershell
.\gradlew.bat runServer
```

Recipes
-------

- Logic Cable: copper ingots plus redstone, yields 8 cables.
- Access Panel: iron ingots, glass pane, redstone, chest, and Logic Cable.
- Crafting Panel: Access Panel, crafting table, and redstone.

Multiplayer
-----------

The mod is intended for modded multiplayer. Server and clients must use the same Minecraft, NeoForge, and mod versions. JEI is optional for the server, but clients need JEI installed if they want to use JEI-specific features.
