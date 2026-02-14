package io.github.projectunified.hytalerun.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Manages the Hytale server child process lifecycle.
 */
public class ServerProcessRunner {

    private final Log log;

    public ServerProcessRunner(Log log) {
        this.log = log;
    }

    /**
     * Launches the server as a child process and waits for it to complete.
     * Registers a shutdown hook to gracefully terminate the server on Ctrl+C.
     *
     * @param command          the command line to execute
     * @param workingDirectory the working directory for the process
     * @throws IOException            if the process cannot be started
     * @throws MojoExecutionException if the server exits with a non-zero code or is
     *                                interrupted
     */
    public void run(List<String> command, File workingDirectory) throws IOException, MojoExecutionException {
        Files.createDirectories(workingDirectory.toPath());

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
}
