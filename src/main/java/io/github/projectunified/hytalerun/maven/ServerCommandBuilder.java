package io.github.projectunified.hytalerun.maven;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the command line for launching the Hytale server.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * List<String> cmd = new ServerCommandBuilder(log)
 *         .serverJar(jar)
 *         .assetsPath(assets)
 *         .modsDirectory(mods)
 *         .authMode("offline")
 *         .debug(true)
 *         .debugPort(5005)
 *         .build();
 * }</pre>
 */
public class ServerCommandBuilder {

    private final Log log;

    private File serverJar;
    private File assetsPath;
    private File modsDirectory;
    private String authMode = "offline";
    private boolean bare;
    private boolean debug;
    private int debugPort = 5005;
    private boolean debugSuspend;
    private List<String> jvmArgs;
    private List<String> bootCommands;
    private List<String> serverArgs;

    public ServerCommandBuilder(Log log) {
        this.log = log;
    }

    public ServerCommandBuilder serverJar(File serverJar) {
        this.serverJar = serverJar;
        return this;
    }

    public ServerCommandBuilder assetsPath(File assetsPath) {
        this.assetsPath = assetsPath;
        return this;
    }

    public ServerCommandBuilder modsDirectory(File modsDirectory) {
        this.modsDirectory = modsDirectory;
        return this;
    }

    public ServerCommandBuilder authMode(String authMode) {
        this.authMode = authMode;
        return this;
    }

    public ServerCommandBuilder bare(boolean bare) {
        this.bare = bare;
        return this;
    }

    public ServerCommandBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public ServerCommandBuilder debugPort(int debugPort) {
        this.debugPort = debugPort;
        return this;
    }

    public ServerCommandBuilder debugSuspend(boolean debugSuspend) {
        this.debugSuspend = debugSuspend;
        return this;
    }

    public ServerCommandBuilder jvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
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

    /**
     * Builds the command line from the configured parameters.
     *
     * @return the command line as a list of strings
     * @throws IllegalStateException if required parameters are missing
     */
    public List<String> build() {
        if (serverJar == null) {
            throw new IllegalStateException("serverJar is required");
        }
        if (assetsPath == null) {
            throw new IllegalStateException("assetsPath is required");
        }
        if (modsDirectory == null) {
            throw new IllegalStateException("modsDirectory is required");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJavaExecutable());

        // JVM arguments
        if (jvmArgs != null) {
            cmd.addAll(jvmArgs);
        }

        // Debug configuration
        if (debug) {
            String suspend = debugSuspend ? "y" : "n";
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=" + suspend + ",address=*:" + debugPort);
            log.info("Debug mode enabled on port " + debugPort + " (suspend=" + suspend + ")");
        }

        // Server JAR
        cmd.add("-jar");
        cmd.add(serverJar.getAbsolutePath());

        // Mods directory
        cmd.add("--mods");
        cmd.add(modsDirectory.getAbsolutePath());

        // Assets
        cmd.add("--assets");
        cmd.add(assetsPath.getAbsolutePath());

        // Auth mode
        cmd.add("--auth-mode");
        cmd.add(authMode);

        // Bare mode
        if (bare) {
            cmd.add("--bare");
        }

        // Boot commands
        if (bootCommands != null && !bootCommands.isEmpty()) {
            cmd.add("--boot-command");
            cmd.add(String.join(",", bootCommands));
        }

        // Additional server arguments
        if (serverArgs != null) {
            cmd.addAll(serverArgs);
        }

        return cmd;
    }

    private String resolveJavaExecutable() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty()) {
            Path javaBin = Path.of(javaHome, "bin", "java");
            if (javaBin.toFile().isFile()) {
                log.info("Using JAVA_HOME: " + javaHome);
                return javaBin.toString();
            }
        }
        return "java";
    }
}
