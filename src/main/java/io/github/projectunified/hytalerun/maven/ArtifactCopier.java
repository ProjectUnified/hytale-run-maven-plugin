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
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Copies the project artifact and its runtime dependencies into the mods
 * directory. Only JARs containing a {@code manifest.json} at the root are
 * copied, since the Hytale plugin system requires it.
 */
public class ArtifactCopier {

    private final Log log;

    public ArtifactCopier(Log log) {
        this.log = log;
    }

    /**
     * Copies the project JAR and runtime dependencies into the target directory.
     * Only artifacts containing {@code manifest.json} are copied.
     * The server artifact ({@code com.hypixel.hytale:Server}) is excluded.
     *
     * @param project   the Maven project
     * @param targetDir the mods directory to copy into
     * @throws IOException          if a file copy fails
     * @throws MojoFailureException if the project artifact is not available
     */
    public void copyArtifacts(MavenProject project, File targetDir) throws IOException, MojoFailureException {
        // Copy the project's own artifact
        Artifact projectArtifact = project.getArtifact();
        if (projectArtifact == null || projectArtifact.getFile() == null) {
            throw new MojoFailureException(
                    "Project artifact not found. Make sure the project is packaged before running this goal.");
        }

        File projectJar = projectArtifact.getFile();
        if (!hasManifest(projectJar)) {
            throw new MojoFailureException(
                    "Project artifact does not contain manifest.json. "
                            + "Hytale plugins require a manifest.json at the JAR root.");
        }
        copyFile(projectJar, targetDir);
        log.info("Copied project artifact: " + projectJar.getName());

        // Copy runtime dependencies that have manifest.json (excluding the server
        // itself)
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (ServerJarResolver.SERVER_GROUP_ID.equals(artifact.getGroupId())
                        && ServerJarResolver.SERVER_ARTIFACT_ID.equals(artifact.getArtifactId())) {
                    continue; // don't copy the server JAR into mods
                }

                String scope = artifact.getScope();
                if (Artifact.SCOPE_COMPILE.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope)) {
                    File file = artifact.getFile();
                    if (file != null && file.isFile() && hasManifest(file)) {
                        copyFile(file, targetDir);
                        log.debug("Copied dependency: " + artifact.getId());
                    }
                }
            }
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
