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
juststoragepanel-1.21.1-1.0.3.jar
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

When the repository URL points to GitHub, the build defaults to GitHub Packages automatically. For this project that means the default Maven target is:

```text
https://maven.pkg.github.com/GeneraBlack/JustStoragePanel
```

In GitHub Actions, `GITHUB_TOKEN` is sufficient for publishing to that default GitHub Packages registry.

The published Maven coordinates use this artifact id:

```text
de.juststoragepanel:juststoragepanel-1.21.1:1.0.3
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
- `republish.yml` reruns Maven and/or CurseForge publishing for an existing branch or tag without creating a new release tag.

Recommended release flow:

1. Create your own Git repository and point `origin` to it.
2. Push the main branch.
3. Create and push a version tag such as `v1.0.3`.
4. Configure `MAVEN_URL`, `MAVEN_PASSWORD`, `CURSEFORGE_PROJECT_ID`, and `CURSEFORGE_TOKEN` in GitHub if you want automated publishing.

If you publish to GitHub Packages for this repository, `MAVEN_URL` is optional because the workflow derives it automatically from the repository name.

CurseForge Verification
-----------------------

If a file upload succeeds but the project page still appears outdated, verify the file from the CurseForge `Files` view instead of only the overview page.

Check these points on CurseForge:

1. Open the project's `Files` tab.
2. Sort by newest files.
3. Set the game version filter to `1.21.1` or show all versions.
4. Set the mod loader filter to `NeoForge` or show all loaders.
5. Hard refresh the page if the newest file is not shown immediately.

If a tagged release ever reaches GitHub but one publishing target still lags behind, use the `Re-Publish` workflow from GitHub Actions and select the affected tag, for example `v1.0.3`.

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
