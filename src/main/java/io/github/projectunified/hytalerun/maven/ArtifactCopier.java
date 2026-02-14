package io.github.projectunified.hytalerun.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Copies the project artifact and its dependencies into the mods directory.
 * Only JARs containing a {@code manifest.json} at the root are copied,
 * since the Hytale plugin system requires it.
 */
public class ArtifactCopier {

    private final Log log;

    public ArtifactCopier(Log log) {
        this.log = log;
    }

    /**
     * Copies the project JAR and optionally its dependencies into the target
     * directory. Only artifacts containing {@code manifest.json} are copied.
     * The server artifact ({@code com.hypixel.hytale:Server}) is always
     * excluded.
     *
     * @param project           the Maven project
     * @param artifactFile      the artifact file to copy into
     * @param targetDir         the mods directory to copy into
     * @param clearMods         whether to clear existing JARs before copying
     * @param copyDependencies  whether to copy dependency artifacts
     * @param allowedScopes     the artifact scopes to include (e.g. compile,
     *                          provided, runtime)
     * @param excludedArtifacts artifacts to exclude, formatted as
     *                          {@code groupId:artifactId}
     * @throws IOException          if a file copy fails
     * @throws MojoFailureException if the project artifact is not available
     */
    public void copyArtifacts(MavenProject project, File artifactFile, File targetDir, boolean clearMods,
                              boolean copyDependencies, List<String> allowedScopes,
                              List<String> excludedArtifacts)
            throws IOException, MojoFailureException {
        // Clear existing JARs from the mods directory
        if (clearMods) {
            clearJars(targetDir);
        }

        // Copy the project's own artifact
        if (!artifactFile.isFile()) {
            throw new MojoFailureException("Artifact file is not a file: " + artifactFile.getAbsolutePath());
        }
        if (!hasManifest(artifactFile)) {
            throw new MojoFailureException(
                    "Artifact file does not contain manifest.json. Hytale plugins require a manifest.json at the JAR root.");
        }
        copyFile(artifactFile, targetDir);
        log.info("Copied project artifact: " + artifactFile.getName());

        // Copy dependencies if enabled
        if (!copyDependencies) {
            log.info("Dependency copying is disabled.");
            return;
        }

        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts == null) {
            return;
        }

        for (Artifact artifact : artifacts) {
            // Always exclude the server JAR
            if (isServerArtifact(artifact)) {
                continue;
            }

            // Check user exclusions
            if (isExcluded(artifact, excludedArtifacts)) {
                log.debug("Excluded by configuration: " + artifact.getId());
                continue;
            }

            // Check scope
            String scope = artifact.getScope();
            if (allowedScopes != null && !allowedScopes.contains(scope)) {
                log.debug("Excluded by scope: " + artifact.getId() + " (" + scope + ")");
                continue;
            }

            File file = artifact.getFile();
            if (file == null || !file.isFile() || !hasManifest(file)) {
                log.debug("Excluded by file checker: " + artifact.getId());
                continue;
            }

            copyFile(file, targetDir);
            log.info("Copied dependency: " + artifact.getId());
        }
    }

    private boolean isServerArtifact(Artifact artifact) {
        return ServerJarResolver.SERVER_GROUP_ID.equals(artifact.getGroupId())
                && ServerJarResolver.SERVER_ARTIFACT_ID.equals(artifact.getArtifactId());
    }

    private boolean isExcluded(Artifact artifact, List<String> excludedArtifacts) {
        if (excludedArtifacts == null || excludedArtifacts.isEmpty()) {
            return false;
        }
        for (String exclusion : excludedArtifacts) {
            String[] parts = exclusion.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String groupPattern = parts[0];
            String artifactPattern = parts[1];
            if (artifact.getGroupId().equals(groupPattern)
                    && matchesWildcard(artifact.getArtifactId(), artifactPattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWildcard(String value, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }
        int starIndex = pattern.indexOf('*');
        if (starIndex < 0) {
            return value.equals(pattern);
        }
        String prefix = pattern.substring(0, starIndex);
        String suffix = pattern.substring(starIndex + 1);
        return value.startsWith(prefix) && value.endsWith(suffix)
                && value.length() >= prefix.length() + suffix.length();
    }

    private void clearJars(File directory) throws IOException {
        File[] jars = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                Files.delete(jar.toPath());
                log.debug("Deleted: " + jar.getName());
            }
            log.info("Cleared " + jars.length + " JAR(s) from " + directory.getAbsolutePath());
        }
    }

    private boolean hasManifest(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            return jar.getEntry("manifest.json") != null;
        } catch (IOException e) {
            log.debug("Could not read JAR " + jarFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void copyFile(File source, File targetDir) throws IOException {
        Path target = targetDir.toPath().resolve(source.getName());
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
