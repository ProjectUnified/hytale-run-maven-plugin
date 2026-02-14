package io.github.projectunified.hytalerun.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the Hytale server as a forked child process.
 * <p>
 * Handles process-level concerns: java executable resolution, JVM arguments,
 * debug agent configuration, and process lifecycle management.
 * <p>
 * When debug is not explicitly enabled, the runner auto-detects if the current
 * Maven JVM is being debugged (e.g. via IntelliJ's Debug button) and
 * automatically configures the forked server process with a JDWP agent.
 */
public class ForkedServerRunner {

    private final Log log;
    private final File serverJar;

    private boolean debug;
    private int debugPort = 5005;
    private boolean debugSuspend;
    private List<String> jvmArgs;

    public ForkedServerRunner(Log log, File serverJar) {
        this.log = log;
        this.serverJar = serverJar;
    }

    public ForkedServerRunner debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public ForkedServerRunner debugPort(int debugPort) {
        this.debugPort = debugPort;
        return this;
    }

    public ForkedServerRunner debugSuspend(boolean debugSuspend) {
        this.debugSuspend = debugSuspend;
        return this;
    }

    public ForkedServerRunner jvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    /**
     * Launches the server as a forked child process and waits for it to
     * complete. Registers a shutdown hook to terminate the server on Ctrl+C.
     *
     * @param serverArgs       the server CLI arguments (from
     *                         {@link ServerCommandBuilder})
     * @param workingDirectory the working directory for the process
     * @throws IOException            if the process cannot be started
     * @throws MojoExecutionException if the server exits with a non-zero code
     *                                or is interrupted
     */
    public void run(List<String> serverArgs, File workingDirectory) throws IOException, MojoExecutionException {
        // Auto-detect debug mode if not explicitly set
        if (!debug && isMavenDebugged()) {
            log.info("Detected that Maven is running in debug mode.");
            log.info("Automatically enabling debug on the forked server (port " + debugPort + ", suspend=y).");
            log.info("Attach IntelliJ's Remote JVM Debug to port " + debugPort + " to debug the server.");
            debug = true;
            debugSuspend = true;
        }

        Files.createDirectories(workingDirectory.toPath());

        List<String> command = buildCommand(serverArgs);

        log.info("Launching Hytale server (forked):");
        log.info(String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory);
        pb.inheritIO();

        Process process = pb.start();

        // Register shutdown hook to kill the server on Ctrl+C
        Thread shutdownHook = new Thread(() -> {
            if (process.isAlive()) {
                log.info("Shutting down Hytale server...");
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    process.destroyForcibly();
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException("Hytale server exited with code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new MojoExecutionException("Server process interrupted", e);
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is already shutting down, ignore
            }
        }
    }

    /**
     * Detects if the current Maven JVM has a JDWP debug agent attached.
     */
    private boolean isMavenDebugged() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.contains("jdwp") || arg.contains("-agentlib:jdwp") || arg.contains("-Xrunjdwp")) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildCommand(List<String> serverArgs) {
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

        // Server arguments
        cmd.addAll(serverArgs);

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
