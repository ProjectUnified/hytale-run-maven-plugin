package io.github.projectunified.hytalerun.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
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
     * Additional mod directories to be included in the server
     */
    @Parameter(property = "hytale.modsDirectory")
    private List<File> modsDirectory;

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
     * Commands to run on server boot.
     */
    @Parameter(property = "hytale.bootCommands")
    private List<String> bootCommands;

    /**
     * Clear all JAR files from the mods directory before copying new ones.
     */
    @Parameter(property = "hytale.clearMods", defaultValue = "true")
    private boolean clearMods;

    /**
     * The artifact file to be used in the server
     */
    @Parameter(property = "hytale.artifactFile", defaultValue = "${project.buildDirectory}/${project.finalName}.jar")
    private File artifactFile;

    /**
     * Whether to copy dependency artifacts into the mods directory.
     */
    @Parameter(property = "hytale.copyDependencies", defaultValue = "true")
    private boolean copyDependencies;

    /**
     * Artifact scopes to include when copying dependencies.
     */
    @Parameter(property = "hytale.allowedScopes")
    private List<String> allowedScopes;

    /**
     * Artifacts to exclude from copying, formatted as
     * {@code groupId:artifactId}.
     */
    @Parameter(property = "hytale.excludedArtifacts")
    private List<String> excludedArtifacts;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File resolvedServerJar = new ServerJarResolver(getLog()).resolve(serverJar, project);
            File resolvedAssets = new AssetsResolver(getLog()).resolve(assetsPath, resolvedServerJar);
            File resolvedModsDir = new File(workingDirectory, "mods");

            // Default scopes if not configured
            if (allowedScopes == null || allowedScopes.isEmpty()) {
                allowedScopes = List.of("compile", "provided", "runtime");
            }

            new ArtifactCopier(getLog()).copyArtifacts(
                    project, artifactFile, resolvedModsDir, clearMods,
                    copyDependencies, allowedScopes, excludedArtifacts);

            // Build server arguments (shared across execution modes)
            List<String> serverArgsList = new ServerCommandBuilder()
                    .assetsPath(resolvedAssets)
                    .authMode(authMode)
                    .disableSentry(disableSentry)
                    .bootCommands(bootCommands)
                    .serverArgs(serverArgs)
                    .modsDirectory(modsDirectory)
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
}
