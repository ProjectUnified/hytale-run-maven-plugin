# Hytale Run Maven Plugin

A Maven plugin that runs a Hytale server with your plugin loaded for
development.

## What it does

1. **Auto-Builds**: Automatically runs the `package` phase of your project
   before executing the server.
2. **Plugin Management**:
   - Copies your project's JAR into the server's `mods/` directory.
   - Optionally copies runtime dependencies (only those containing a
     `manifest.json`).
   - Supports clearing existing JARs in the `mods/` directory before copying.
   - Allows filtering dependencies by scope and explicit exclusions (with
     wildcards).
3. **Process Management**: Launches the Hytale server as a forked child process.
4. **Auto-Debug**: Detects if Maven is being debugged and automatically
   configures the server for remote debugging.

## Usage

Add the plugin to your project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.projectunified</groupId>
            <artifactId>hytale-run-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <configuration>
                <authMode>authenticated</authMode>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then run:

```bash
mvn hytale-run:run
```

## Configuration

| Parameter           | Property                   | Default                      | Description                                                   |
|:--------------------|:---------------------------|:-----------------------------|:--------------------------------------------------------------|
| `serverJar`         | `hytale.serverJar`         | _(auto-resolved)_            | Path to the Hytale server JAR                                 |
| `assetsPath`        | `hytale.assetsPath`        | _(auto-resolved)_            | Path to assets directory/zip                                  |
| `artifactFile`      | `hytale.artifactFile`      | `target/*.jar`               | The project JAR file to be used in the server                 |
| `workingDirectory`  | `hytale.workingDirectory`  | `target/hytale`              | Server working directory                                      |
| `modsDirectory`     | `hytale.modsDirectory`     | _(none)_                     | Additional mod directories to include (List of Files)         |
| `authMode`          | `hytale.authMode`          | `authenticated`              | Auth mode (`authenticated`, `offline`, `insecure`)            |
| `debug`             | `hytale.debug`             | `false`                      | Manually enable remote JDWP debugging                         |
| `debugPort`         | `hytale.debugPort`         | `5005`                       | Debug port                                                    |
| `debugSuspend`      | `hytale.debugSuspend`      | `false`                      | Suspend until debugger attaches                               |
| `jvmArgs`           | `hytale.jvmArgs`           | _(empty)_                    | Extra JVM arguments for the server (List of Strings)          |
| `serverArgs`        | `hytale.serverArgs`        | _(empty)_                    | Extra server CLI arguments (List of Strings)                  |
| `disableSentry`     | `hytale.disableSentry`     | `true`                       | Disable Sentry error reporting                                |
| `bootCommands`      | `hytale.bootCommands`      | _(none)_                     | Commands to run on server boot (List of Strings)              |
| `clearMods`         | `hytale.clearMods`         | `true`                       | Clear `mods/` directory before copying                        |
| `copyDependencies`  | `hytale.copyDependencies`  | `true`                       | Copy plugin dependencies to `mods/`                           |
| `allowedScopes`     | `hytale.allowedScopes`     | `compile, provided, runtime` | Scopes of dependencies to copy (List of Strings)              |
| `excludedArtifacts` | `hytale.excludedArtifacts` | _(none)_                     | Artifacts to exclude (`groupId:artifactId`) (List of Strings) |

### Dependency Filtering

You can exclude specific artifacts from being copied to the `mods/` directory.
ArtifactId supports wildcards (`*`):

```xml
<configuration>
    <excludedArtifacts>
        <exclude>com.example:unwanted-lib</exclude>
        <exclude>io.github.*:*</exclude>
        <exclude>org.apache:*-core</exclude>
    </excludedArtifacts>
</configuration>
```

## Example: Full Configuration

Below is a sample configuration showing all available options used together:

```xml
<configuration>
    <!-- Basic Paths -->
    <serverJar>/path/to/HytaleServer.jar</serverJar>
    <assetsPath>/path/to/Assets.zip</assetsPath>
    <artifactFile>${project.build.directory}/${project.build.finalName}.jar</artifactFile>
    <workingDirectory>${project.build.directory}/hytale</workingDirectory>
    
    <!-- Additional mods directories to include (optional) -->
    <modsDirectory>
        <param>/path/to/other/mods</param>
    </modsDirectory>

    <!-- Server Settings -->
    <authMode>authenticated</authMode>
    <disableSentry>true</disableSentry>
    
    <!-- Debugging (Optional: auto-detects if Maven is debugged) -->
    <debug>false</debug>
    <debugPort>5005</debugPort>
    <debugSuspend>false</debugSuspend>
    
    <!-- Arguments -->
    <jvmArgs>
        <jvmArg>-Xmx4G</jvmArg>
    </jvmArgs>
    <serverArgs>
        <serverArg>--dev</serverArg>
    </serverArgs>
    <bootCommands>
        <bootCommand>op PlayerName</bootCommand>
    </bootCommands>

    <!-- Dependency & Copy Management -->
    <clearMods>true</clearMods>
    <copyDependencies>true</copyDependencies>
    <allowedScopes>
        <scope>compile</scope>
        <scope>runtime</scope>
    </allowedScopes>
    <excludedArtifacts>
        <exclude>com.example:unwanted-lib</exclude>
        <exclude>org.apache:*-core</exclude>
    </excludedArtifacts>
</configuration>
```

## Debugging

The plugin features **Auto-Detect Debug**. If you run Maven in debug mode (e.g.,
via IntelliJ IDEA's "Debug" button), the plugin will:

1. Automatically enable debug mode on the forked server.
2. Set `suspend=y` so the server waits for you.
3. Prompt you to attach your IDE's debugger to the specified `debugPort`
   (default 5005).

To debug, create a **Remote JVM Debug** configuration in your IDE targeting
`localhost:5005`.

## Server JAR Resolution

If `serverJar` is not configured, the plugin resolves it by:

1. Checking for a `com.hypixel.hytale:Server` dependency in the project.
2. Checking platform-specific default paths (e.g., `%APPDATA%/Hytale/...` on
   Windows).
