# Hytale Run Maven Plugin

A Maven plugin that runs a Hytale server with your plugin loaded for
development.

## What it does

1. Copies your project's JAR and its runtime dependencies into a `mods/`
   directory
2. Launches the Hytale server as a child process with the mods loaded
3. Optionally enables remote JDWP debugging

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
                <!-- Optional: server JAR is auto-resolved (see below) -->
                <!-- <serverJar>/path/to/HytaleServer.jar</serverJar> -->
                <authMode>offline</authMode>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then run:

```bash
mvn package hytale-run:run
```

## Server JAR Resolution

If `serverJar` is not explicitly configured, the plugin resolves it
automatically:

1. **Dependency lookup** — scans project dependencies for
   `com.hypixel.hytale:Server`
2. **Default install path** — checks the platform-specific Hytale installation:
   - **Windows**:
     `%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`
   - **Linux**:
     `$XDG_DATA_HOME/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`
   - **macOS**:
     `~/Application Support/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`

## Assets Resolution

If `assetsPath` is not explicitly configured, the plugin looks for `Assets.zip`
relative to the resolved server JAR (at `<serverDir>/../Assets.zip`).

## Configuration

| Parameter          | Property                  | Default             | Description                                        |
| ------------------ | ------------------------- | ------------------- | -------------------------------------------------- |
| `serverJar`        | `hytale.serverJar`        | _(auto-resolved)_   | Path to the Hytale server JAR                      |
| `assetsPath`       | `hytale.assetsPath`       | _(auto-resolved)_   | Path to assets directory/zip                       |
| `workingDirectory` | `hytale.workingDirectory` | `target/hytale`     | Server working directory                           |
| `modsDirectory`    | `hytale.modsDirectory`    | `<workingDir>/mods` | Directory for plugin JARs                          |
| `authMode`         | `hytale.authMode`         | `offline`           | Auth mode (`authenticated`, `offline`, `insecure`) |
| `debug`            | `hytale.debug`            | `false`             | Enable remote JDWP debugging                       |
| `debugPort`        | `hytale.debugPort`        | `5005`              | Debug port                                         |
| `debugSuspend`     | `hytale.debugSuspend`     | `false`             | Suspend until debugger attaches                    |
| `jvmArgs`          | `hytale.jvmArgs`          | _(empty)_           | Extra JVM arguments                                |
| `serverArgs`       | `hytale.serverArgs`       | _(empty)_           | Extra server CLI arguments                         |
| `bare`             | `hytale.bare`             | `false`             | Run in bare mode (no worlds/ports)                 |
| `bootCommands`     | `hytale.bootCommands`     | _(empty)_           | Commands to run on server boot                     |

## Debug Mode

Enable remote debugging to attach your IDE's debugger:

```xml
<configuration>
    <debug>true</debug>
    <debugPort>5005</debugPort>
    <debugSuspend>true</debugSuspend>
</configuration>
```

This adds `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005`
to the JVM launch arguments.

## Example: Full Configuration

```xml
<configuration>
    <serverJar>/path/to/HytaleServer.jar</serverJar>
    <assetsPath>/path/to/Assets.zip</assetsPath>
    <authMode>offline</authMode>
    <bare>true</bare>
    <debug>true</debug>
    <debugPort>5005</debugPort>
    <debugSuspend>false</debugSuspend>
    <jvmArgs>
        <jvmArg>-Xmx4G</jvmArg>
    </jvmArgs>
    <bootCommands>
        <bootCommand>op PlayerName</bootCommand>
    </bootCommands>
</configuration>
```
