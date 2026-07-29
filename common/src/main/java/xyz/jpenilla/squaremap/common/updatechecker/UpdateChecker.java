package xyz.jpenilla.squaremap.common.updatechecker;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.jar.Manifest;
import net.minecraft.SharedConstants;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public record UpdateChecker(Logger logger) {
    private static final String GITHUB_REPO = "jpenilla/squaremap";
    private static final String MODRINTH_PROJECT = "PFb7ZqK6";
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;
    static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public void checkVersion() {
        this.logger.info(Messages.UPDATE_CHECKER_FETCHING_VERSION_INFORMATION);

        final @Nullable Manifest manifest = Util.manifest(UpdateChecker.class); // we expect to be shaded into platform jars
        if (manifest == null) {
            this.logger.warn("Failed to locate manifest, cannot check for updates.");
            return;
        }

        final String currentVersion = manifest.getMainAttributes().getValue("squaremap-version");
        if (currentVersion.contains("-SNAPSHOT")) {
            new GitHubSnapshotUpdateChecker(
                this.logger,
                GITHUB_REPO,
                currentVersion,
                manifest.getMainAttributes().getValue("squaremap-commit"),
                manifest.getMainAttributes().getValue("squaremap-branch")
            ).checkVersion();
        } else {
            new ModrinthUpdateChecker(
                this.logger,
                MODRINTH_PROJECT,
                currentVersion,
                SharedConstants.getCurrentVersion().name()
            ).checkVersion();
        }
    }

    static String userAgent(final String currentVersion) {
        return "squaremap/%s (https://github.com/jpenilla/squaremap)".formatted(currentVersion);
    }
}
