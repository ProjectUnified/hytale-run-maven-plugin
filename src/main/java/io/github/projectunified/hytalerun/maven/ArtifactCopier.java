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
    public void copyArtifacts(MavenProject project, File targetDir, boolean clearMods)
            throws IOException, MojoFailureException {
        // Clear existing JARs from the mods directory
        if (clearMods) {
            clearJars(targetDir);
        }

        // Copy the project's own artifact
        File projectJar = resolveProjectArtifact(project);
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

    private File resolveProjectArtifact(MavenProject project) throws MojoFailureException {
        // Try the normal artifact file first
        Artifact projectArtifact = project.getArtifact();
        if (projectArtifact != null && projectArtifact.getFile() != null && projectArtifact.getFile().isFile()) {
            return projectArtifact.getFile();
        }

        // Fall back to the expected build output (needed when @Execute forks the
        // lifecycle)
        String buildDir = project.getBuild().getDirectory();
        String finalName = project.getBuild().getFinalName();
        String packaging = project.getPackaging();
        File expectedJar = new File(buildDir, finalName + "." + packaging);
        if (expectedJar.isFile()) {
            log.debug("Resolved project artifact from build output: " + expectedJar.getAbsolutePath());
            return expectedJar;
        }

        throw new MojoFailureException(
                "Project artifact not found. Make sure the project is packaged before running this goal.");
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
