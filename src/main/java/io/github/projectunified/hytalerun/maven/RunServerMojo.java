package io.github.projectunified.hytalerun.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Runs a Hytale server with the project's plugin artifacts loaded.
 * <p>
 * Copies the project JAR and its dependencies into a mods directory,
 * then launches the Hytale server as a forked child process with the
 * appropriate CLI options.
 * <p>
 * When Maven is running in debug mode (e.g. IntelliJ's Debug button), the
 * plugin automatically configures the forked server with a JDWP agent so you
 * can attach a Remote JVM Debug configuration.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class RunServerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Path to the Hytale server JAR.
     * If not specified, the plugin will:
     * <ol>
     * <li>Check the platform-specific default Hytale installation path</li>
     * <li>Look for a {@code com.hypixel.hytale:Server} dependency in the
     * project</li>
     * </ol>
     */
    @Parameter(property = "hytale.serverJar")
    private File serverJar;

    /**
     * Path to the assets directory or zip file ({@code --assets}).
     * Required for the server to run.
     * If not specified, the plugin will look for {@code Assets.zip} next to the
     * resolved server JAR.
     */
    @Parameter(property = "hytale.assetsPath")
    private File assetsPath;

    /**
     * Server working directory.
     */
    @Parameter(property = "hytale.workingDirectory", defaultValue = "${project.build.directory}/hytale")
    private File workingDirectory;

    /**
     * Directory where plugin JARs are copied.
     * Defaults to {@code <workingDirectory>/mods} if not specified.
     */
    @Parameter(property = "hytale.modsDirectory")
    private File modsDirectory;

    /**
     * Authentication mode: {@code authenticated}, {@code offline}, or
     * {@code insecure}.
     */
    @Parameter(property = "hytale.authMode", defaultValue = "authenticated")
    private String authMode;

    /**
     * Enable remote JDWP debugging on the forked server process.
     * When not set, the plugin auto-detects if Maven is being debugged
     * and enables it automatically.
     */
    @Parameter(property = "hytale.debug", defaultValue = "false")
    private boolean debug;

    /**
     * Debug port for remote debugging.
     */
    @Parameter(property = "hytale.debugPort", defaultValue = "5005")
    private int debugPort;

    /**
     * Whether to suspend the JVM on start until a debugger attaches.
     */
    @Parameter(property = "hytale.debugSuspend", defaultValue = "false")
    private boolean debugSuspend;

    /**
     * Additional JVM arguments for the forked server process.
     */
    @Parameter(property = "hytale.jvmArgs")
    private List<String> jvmArgs;

    /**
     * Additional server CLI arguments.
     */
    @Parameter(property = "hytale.serverArgs")
    private List<String> serverArgs;

    /**
     * Disable sentry report to Hytale
     */
    @Parameter(property = "hytale.disableSentry", defaultValue = "true")
    private boolean disableSentry;

    /**
     * Commands to run on server boot (comma-separated when passed via CLI).
     */
    @Parameter(property = "hytale.bootCommands")
    private List<String> bootCommands;

    /**
     * Clear all JAR files from the mods directory before copying new ones.
     */
    @Parameter(property = "hytale.clearMods", defaultValue = "true")
    private boolean clearMods;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File resolvedServerJar = new ServerJarResolver(getLog()).resolve(serverJar, project);
            File resolvedAssets = new AssetsResolver(getLog()).resolve(assetsPath, resolvedServerJar);
            File resolvedModsDir = resolveModsDirectory();

            new ArtifactCopier(getLog()).copyArtifacts(project, resolvedModsDir, clearMods);

            // Build server arguments (shared across execution modes)
            List<String> serverArgsList = new ServerCommandBuilder()
                    .assetsPath(resolvedAssets)
                    .authMode(authMode)
                    .disableSentry(disableSentry)
                    .bootCommands(bootCommands)
                    .serverArgs(serverArgs)
                    .build();

            new ForkedServerRunner(getLog(), resolvedServerJar)
                    .debug(debug)
                    .debugPort(debugPort)
                    .debugSuspend(debugSuspend)
                    .jvmArgs(jvmArgs)
                    .run(serverArgsList, workingDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to run Hytale server", e);
        }
    }

    private File resolveModsDirectory() throws IOException {
        if (modsDirectory == null) {
            modsDirectory = new File(workingDirectory, "mods");
        }
        Files.createDirectories(modsDirectory.toPath());
        return modsDirectory;
    }
}
