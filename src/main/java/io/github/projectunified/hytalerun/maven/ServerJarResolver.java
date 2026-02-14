package io.github.projectunified.hytalerun.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves the Hytale server JAR location.
 * <p>
 * Resolution order:
 * <ol>
 * <li>Explicitly configured path</li>
 * <li>Platform-specific default installation path</li>
 * <li>Project dependency ({@code com.hypixel.hytale:Server})</li>
 * </ol>
 */
public class ServerJarResolver {

    static final String SERVER_GROUP_ID = "com.hypixel.hytale";
    static final String SERVER_ARTIFACT_ID = "Server";

    private final Log log;

    public ServerJarResolver(Log log) {
        this.log = log;
    }

    /**
     * Resolves the server JAR file.
     *
     * @param serverJar explicitly configured path, or {@code null}
     * @param project   the Maven project for dependency lookup
     * @return the resolved server JAR file
     * @throws MojoFailureException if the server JAR cannot be found
     */
    public File resolve(File serverJar, MavenProject project) throws MojoFailureException {
        // 1. Explicitly configured
        if (serverJar != null) {
            if (!serverJar.isFile()) {
                throw new MojoFailureException("Specified serverJar does not exist: " + serverJar.getAbsolutePath());
            }
            log.info("Using configured server JAR: " + serverJar.getAbsolutePath());
            return serverJar;
        }

        // 2. Check default installation path
        File fromInstall = findInDefaultInstall();
        if (fromInstall != null) {
            log.info("Found server JAR at default install location: " + fromInstall.getAbsolutePath());
            return fromInstall;
        }

        // 3. Look in project dependencies
        File fromDependency = findInDependencies(project);
        if (fromDependency != null) {
            log.info("Found server JAR from dependency: " + fromDependency.getAbsolutePath());
            return fromDependency;
        }

        throw new MojoFailureException(
                "Cannot find Hytale server JAR. Please either:\n"
                        + "  - Set <serverJar> in the plugin configuration\n"
                        + "  - Add com.hypixel.hytale:Server as a project dependency\n"
                        + "  - Install Hytale to the default location");
    }

    private File findInDependencies(MavenProject project) {
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts == null)
            return null;

        for (Artifact artifact : artifacts) {
            if (SERVER_GROUP_ID.equals(artifact.getGroupId())
                    && SERVER_ARTIFACT_ID.equals(artifact.getArtifactId())) {
                File file = artifact.getFile();
                if (file != null && file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }

    private File findInDefaultInstall() {
        String relPath = "Hytale/install/release/package/game/latest/Server/HytaleServer.jar";
        List<Path> candidates = new ArrayList<>();

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            // Windows: %APPDATA%/Hytale/...
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                candidates.add(Path.of(appData, relPath));
            }
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            // macOS: ~/Application Support/Hytale/...
            String home = System.getProperty("user.home");
            if (home != null) {
                candidates.add(Path.of(home, "Application Support", relPath));
            }
        } else {
            // Linux: $XDG_DATA_HOME/Hytale/... or ~/.local/share/Hytale/...
            String xdgData = System.getenv("XDG_DATA_HOME");
            if (xdgData != null) {
                candidates.add(Path.of(xdgData, relPath));
            }
            String home = System.getProperty("user.home");
            if (home != null) {
                candidates.add(Path.of(home, ".local", "share", relPath));
            }
        }

        for (Path candidate : candidates) {
            File f = candidate.toFile();
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }
}
