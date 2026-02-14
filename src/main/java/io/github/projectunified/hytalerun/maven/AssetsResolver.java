package io.github.projectunified.hytalerun.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

/**
 * Resolves the Hytale assets path.
 * <p>
 * Resolution order:
 * <ol>
 * <li>Explicitly configured path</li>
 * <li>{@code Assets.zip} sibling to the server JAR's parent directory</li>
 * </ol>
 */
public class AssetsResolver {

    private final Log log;

    public AssetsResolver(Log log) {
        this.log = log;
    }

    /**
     * Resolves the assets path.
     *
     * @param assetsPath        explicitly configured path, or {@code null}
     * @param resolvedServerJar the resolved server JAR (used for sibling lookup)
     * @return the resolved assets file/directory
     * @throws MojoFailureException if assets cannot be found
     */
    public File resolve(File assetsPath, File resolvedServerJar) throws MojoFailureException {
        // 1. Explicitly configured
        if (assetsPath != null) {
            if (!assetsPath.exists()) {
                throw new MojoFailureException("Specified assetsPath does not exist: " + assetsPath.getAbsolutePath());
            }
            log.info("Using configured assets path: " + assetsPath.getAbsolutePath());
            return assetsPath;
        }

        // 2. Look for Assets.zip relative to the server JAR
        // Server JAR is at .../latest/Server/HytaleServer.jar
        // Assets.zip could be at .../latest/Server/Assets.zip (same dir)
        // or at .../latest/Assets.zip (parent dir)
        File serverParent = resolvedServerJar.getParentFile();
        if (serverParent != null) {
            File assetsZipSameDir = new File(serverParent, "Assets.zip");
            if (assetsZipSameDir.isFile()) {
                log.info("Found assets at: " + assetsZipSameDir.getAbsolutePath());
                return assetsZipSameDir;
            }

            File gameDir = serverParent.getParentFile();
            if (gameDir != null) {
                File assetsZipParentDir = new File(gameDir, "Assets.zip");
                if (assetsZipParentDir.isFile()) {
                    log.info("Found assets at: " + assetsZipParentDir.getAbsolutePath());
                    return assetsZipParentDir;
                }
            }
        }

        throw new MojoFailureException(
                "Cannot find Hytale assets. Please set <assetsPath> in the plugin configuration.\n"
                        + "The assets file (Assets.zip) is required for the server to run.");
    }
}
