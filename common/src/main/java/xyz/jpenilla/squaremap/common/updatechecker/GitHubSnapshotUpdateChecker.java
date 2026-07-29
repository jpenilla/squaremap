package xyz.jpenilla.squaremap.common.updatechecker;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
record GitHubSnapshotUpdateChecker(Logger logger, String githubRepo, String currentVersion, String gitHash, String branch) {
    public void checkVersion() {
        final Distance result = this.fetchDistance();

        switch (result) {
            case Distance.UpToDate $ -> {}
            case Distance.Behind behind -> {
                this.logger.info(
                    Messages.UPDATE_CHECKER_BEHIND_BRANCH
                        .replace("<branch>", this.branch)
                        .replace("<behind>", String.valueOf(behind.result()))
                );
                this.logger.info(Messages.UPDATE_CHECKER_DOWNLOAD_DEV_BUILDS.replace("<link>", "https://jenkins.jpenilla.xyz/job/squaremap/"));
            }
            case Distance.Failure failure -> this.logger.warn("Error obtaining version information", failure.reason());
            case Distance.Ahead $ -> this.logger.info("Commit '{}' is ahead of branch '{}', local build?", this.gitHash, this.branch);
            case Distance.Diverged $ -> this.logger.info("Commit '{}' is diverged from branch '{}', local build?", this.gitHash, this.branch);
            case Distance.UnknownCommit $ -> this.logger.info(Messages.UPDATE_CHECKER_UNKNOWN_COMMIT.replace("<commit>", this.gitHash));
        }
    }

    private Distance fetchDistance() {
        final URI uri = URI.create("https://api.github.com/repos/%s/compare/%s...%s".formatted(this.githubRepo, this.branch, this.gitHash));
        final HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(UpdateChecker.REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .header("User-Agent", UpdateChecker.userAgent(this.currentVersion))
            .GET()
            .build();

        final HttpResponse<InputStream> response;
        try {
            response = UpdateChecker.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (final IOException e) {
            return new Distance.Failure(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Distance.Failure(e);
        }

        final byte[] responseBytes;
        try (InputStream body = response.body()) {
            if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return new Distance.UnknownCommit();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new Distance.Failure(new IOException("GitHub returned HTTP " + response.statusCode()));
            }
            responseBytes = body.readNBytes(UpdateChecker.MAX_RESPONSE_SIZE + 1);
        } catch (final IOException e) {
            return new Distance.Failure(e);
        }
        if (responseBytes.length > UpdateChecker.MAX_RESPONSE_SIZE) {
            return new Distance.Failure(new IOException("GitHub response exceeded maximum size of " + UpdateChecker.MAX_RESPONSE_SIZE + " bytes"));
        }

        try {
            final JsonObject responseJson = Util.gson().fromJson(new String(responseBytes, StandardCharsets.UTF_8), JsonObject.class);
            final String status = responseJson.get("status").getAsString();
            return switch (status) {
                case "identical" -> new Distance.UpToDate();
                case "behind" -> new Distance.Behind(responseJson.get("behind_by").getAsInt());
                case "ahead" -> new Distance.Ahead();
                case "diverged" -> new Distance.Diverged();
                default -> new Distance.Failure(new IllegalArgumentException("Unknown status: '" + status + "'"));
            };
        } catch (final JsonSyntaxException | NumberFormatException e) {
            return new Distance.Failure(e);
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

        final class Ahead implements Distance {
        }

        final class Diverged implements Distance {
        }
    }
}
