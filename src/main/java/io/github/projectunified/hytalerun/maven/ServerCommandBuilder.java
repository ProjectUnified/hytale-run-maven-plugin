package io.github.projectunified.hytalerun.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the server CLI arguments shared by both forked and in-process runners.
 * <p>
 * This builder only produces server-level arguments (assets, auth mode,
 * boot commands, etc.). Process-level options (JVM args, debug, java
 * executable)
 * are handled by the runner implementations.
 * <p>
 * Usage:
 *
 * <pre>{@code
 * List<String> args = new ServerCommandBuilder()
 *         .assetsPath(assets)
 *         .authMode("offline")
 *         .build();
 * }</pre>
 */
public class ServerCommandBuilder {

    private File assetsPath;
    private String authMode = "offline";
    private boolean disableSentry;
    private List<String> bootCommands;
    private List<String> serverArgs;
    private List<File> modsDirectory;

    public ServerCommandBuilder assetsPath(File assetsPath) {
        this.assetsPath = assetsPath;
        return this;
    }

    public ServerCommandBuilder authMode(String authMode) {
        this.authMode = authMode;
        return this;
    }

    public ServerCommandBuilder disableSentry(boolean disableSentry) {
        this.disableSentry = disableSentry;
        return this;
    }

    public ServerCommandBuilder bootCommands(List<String> bootCommands) {
        this.bootCommands = bootCommands;
        return this;
    }

    public ServerCommandBuilder serverArgs(List<String> serverArgs) {
        this.serverArgs = serverArgs;
        return this;
    }

    public ServerCommandBuilder modsDirectory(List<File> modsDirectory) {
        this.modsDirectory = modsDirectory;
        return this;
    }

    /**
     * Builds the server CLI arguments.
     *
     * @return the argument list
     * @throws IllegalStateException if required parameters are missing
     */
    public List<String> build() {
        if (assetsPath == null) {
            throw new IllegalStateException("assetsPath is required");
        }

        List<String> args = new ArrayList<>();

        // Assets
        args.add("--assets");
        args.add(assetsPath.getAbsolutePath());

        // Auth mode
        args.add("--auth-mode");
        args.add(authMode);

        // Disable Sentry
        if (disableSentry) {
            args.add("--disable-sentry");
        }

        // Additional mods directories
        if (modsDirectory != null && !modsDirectory.isEmpty()) {
            for (File modsDirectory : modsDirectory) {
                args.add("--mods");
                args.add(modsDirectory.getAbsolutePath());
            }
        }

        // Boot commands
        if (bootCommands != null && !bootCommands.isEmpty()) {
            for (String bootCommand : bootCommands) {
                args.add("--boot-command");
                args.add(bootCommand);
            }
        }

        // Additional server arguments
        if (serverArgs != null) {
            args.addAll(serverArgs);
        }

        return args;
    }
}
