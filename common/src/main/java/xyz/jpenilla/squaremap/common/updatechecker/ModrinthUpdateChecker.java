package xyz.jpenilla.squaremap.common.updatechecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
record ModrinthUpdateChecker(Logger logger, String project, String currentVersion, String minecraftVersion) {
    public void checkVersion() {
        final List<Release> releases;
        try {
            releases = this.fetchReleases();
        } catch (final IOException e) {
            this.logger.warn("Failed to list releases, cannot check for updates.", e);
            return;
        }

        if (releases.isEmpty()) {
            this.logger.warn("No releases found for Minecraft {}.", this.minecraftVersion);
            return;
        }

        final Release latest = releases.getFirst();
        if (latest.version().equals(this.currentVersion)) {
            return;
        }

        int versionsBehind = -1;
        for (int i = 0; i < releases.size(); i++) {
            if (releases.get(i).version().equals(this.currentVersion)) {
                versionsBehind = i;
                break;
            }
        }
        this.logger.info(Messages.UPDATE_CHECKER_BEHIND_RELEASES.replace("<behind>", versionsBehind == -1 ? "?" : String.valueOf(versionsBehind)));
        this.logger.info(
            Messages.UPDATE_CHECKER_DOWNLOAD_RELEASE
                .replace("<latest>", "v" + latest.version())
                .replace("<link>", "https://modrinth.com/project/%s/versions".formatted(this.project))
        );
    }

    private List<Release> fetchReleases() throws IOException {
        final String gameVersions = URLEncoder.encode(Util.gson().toJson(List.of(this.minecraftVersion)), StandardCharsets.UTF_8);
        final URI uri = URI.create(
            "https://api.modrinth.com/v2/project/%s/version?game_versions=%s&include_changelog=false"
                .formatted(this.project, gameVersions)
        );
        final HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(UpdateChecker.REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .header("User-Agent", UpdateChecker.userAgent(this.currentVersion))
            .GET()
            .build();

        final HttpResponse<InputStream> response;
        try {
            response = UpdateChecker.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching releases from Modrinth", e);
        }

        final byte[] responseBytes;
        try (InputStream body = response.body()) {
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Modrinth returned HTTP " + response.statusCode());
            }
            responseBytes = body.readNBytes(UpdateChecker.MAX_RESPONSE_SIZE + 1);
        }
        if (responseBytes.length > UpdateChecker.MAX_RESPONSE_SIZE) {
            throw new IOException("Modrinth response exceeded maximum size of " + UpdateChecker.MAX_RESPONSE_SIZE + " bytes");
        }

        final JsonArray result;
        try {
            result = Util.gson().fromJson(new String(responseBytes, StandardCharsets.UTF_8), JsonArray.class);
        } catch (final JsonSyntaxException e) {
            throw new IOException("Invalid response from Modrinth", e);
        }

        final Map<String, Release> releasesByVersion = new HashMap<>();
        try {
            for (final JsonElement element : result) {
                final JsonObject releaseJson = element.getAsJsonObject();
                if (!releaseJson.get("version_type").getAsString().equals("release")) {
                    continue;
                }
                final Release release = new Release(
                    releaseJson.get("version_number").getAsString(),
                    Instant.parse(releaseJson.get("date_published").getAsString())
                );
                releasesByVersion.merge(
                    release.version(),
                    release,
                    (existing, replacement) -> existing.published().isAfter(replacement.published()) ? existing : replacement
                );
            }
        } catch (final RuntimeException e) {
            throw new IOException("Invalid response from Modrinth", e);
        }

        final List<Release> releases = new ArrayList<>(releasesByVersion.values());
        releases.sort(Comparator.comparing(Release::published).reversed());
        return releases;
    }

    private record Release(String version, Instant published) {
    }
}
