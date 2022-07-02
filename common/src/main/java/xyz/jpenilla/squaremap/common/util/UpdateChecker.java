package xyz.jpenilla.squaremap.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Messages;

@DefaultQualifier(NonNull.class)
public record UpdateChecker(Logger logger, String githubRepo) {
    public void checkVersion() {
        this.logger.info(Messages.UPDATE_CHECKER_FETCHING_VERSION_INFORMATION);

        final @Nullable Manifest manifest = manifest(UpdateChecker.class); // we expect to be shaded into platform jars
        if (manifest == null) {
            this.logger.warn("Failed to locate manifest, cannot check for updates.");
            return;
        }

        final String currentVersion = manifest.getMainAttributes().getValue("squaremap-version");

        if (currentVersion.contains("-SNAPSHOT")) {
            this.checkSnapshot(manifest);
            return;
        }

        this.checkRelease(currentVersion);
    }

    private void checkSnapshot(final Manifest manifest) {
        final String gitHash = manifest.getMainAttributes().getValue("squaremap-commit");
        final String branch = manifest.getMainAttributes().getValue("squaremap-branch");
        final Distance result = this.fetchDistance(branch, gitHash);

        if (result instanceof Distance.UpToDate) {
            return;
        } else if (result instanceof Distance.Behind behind) {
            this.logger.info(
                Messages.UPDATE_CHECKER_BEHIND_BRANCH
                    .replace("<branch>", branch)
                    .replace("<behind>", String.valueOf(behind.result()))
            );
            this.logger.info(Messages.UPDATE_CHECKER_DOWNLOAD_DEV_BUILDS.replace("<link>", "https://jenkins.jpenilla.xyz/job/squaremap/"));
        } else if (result instanceof Distance.Failure failure) {
            this.logger.warn("Error obtaining version information", failure.reason());
        } else if (result instanceof Distance.UnknownCommit) {
            this.logger.info(Messages.UPDATE_CHECKER_UNKNOWN_COMMIT.replace("<commit>", gitHash));
        } else {
            throw new RuntimeException("Unknown result type?");
        }
    }

    private void checkRelease(final String currentVersion) {
        final Releases releases;
        try {
            releases = this.fetchReleases();
        } catch (final IOException e) {
            this.logger.warn("Failed to list releases, cannot check for updates.", e);
            return;
        }

        final String ver = "v" + currentVersion;
        if (releases.releaseList().get(0).equals(ver)) {
            return;
        }
        final int versionsBehind = releases.releaseList().indexOf(ver);
        this.logger.info(Messages.UPDATE_CHECKER_BEHIND_RELEASES.replace("<behind>", String.valueOf(versionsBehind == -1 ? "?" : versionsBehind)));
        this.logger.info(
            Messages.UPDATE_CHECKER_DOWNLOAD_RELEASE
                .replace("<latest>", releases.releaseList().get(0))
                .replace("<link>", releases.releaseUrls().get(releases.releaseList().get(0)))
        );
    }

    private Releases fetchReleases() throws IOException {
        final JsonArray result;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/%s/releases".formatted(this.githubRepo)).openStream(), StandardCharsets.UTF_8))) {
            result = Util.gson().fromJson(reader, JsonArray.class);
        }

        final Map<String, String> versionMap = new LinkedHashMap<>();
        for (final JsonElement element : result) {
            versionMap.put(
                element.getAsJsonObject().get("tag_name").getAsString(),
                element.getAsJsonObject().get("html_url").getAsString()
            );
        }
        return new Releases(new ArrayList<>(versionMap.keySet()), versionMap);
    }

    private Distance fetchDistance(final String branch, final String hash) {
        try {
            final URL url = new URL("https://api.github.com/repos/%s/compare/%s...%s".formatted(this.githubRepo, branch, hash));
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return new Distance.UnknownCommit();
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                final JsonObject response = Util.gson().fromJson(reader, JsonObject.class);
                final String status = response.get("status").getAsString();
                return switch (status) {
                    case "identical" -> new Distance.UpToDate();
                    case "behind" -> new Distance.Behind(response.get("behind_by").getAsInt());
                    default -> new Distance.Failure(new IllegalArgumentException("Unknown status: '" + status + "'"));
                };
            }
        } catch (final IOException | JsonSyntaxException | NumberFormatException ex) {
            return new Distance.Failure(ex);
        }
    }

    private static @Nullable Manifest manifest(final Class<?> clazz) {
        final String classLocation = "/" + clazz.getName().replace(".", "/") + ".class";
        final @Nullable URL resource = clazz.getResource(classLocation);
        if (resource == null) {
            return null;
        }
        final String classFilePath = resource.toString().replace("\\", "/");
        final String archivePath = classFilePath.substring(0, classFilePath.length() - classLocation.length());
        try (final InputStream stream = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream()) {
            return new Manifest(stream);
        } catch (final IOException ex) {
            return null;
        }
    }

    private sealed interface Distance {
        record Failure(Exception reason) implements Distance {
        }

        record Behind(int result) implements Distance {
        }

        final class UnknownCommit implements Distance {
        }

        final class UpToDate implements Distance {
        }
    }

    private record Releases(List<String> releaseList, Map<String, String> releaseUrls) {
    }
}
