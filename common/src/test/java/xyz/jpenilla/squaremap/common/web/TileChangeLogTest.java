package xyz.jpenilla.squaremap.common.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.common.coordinate.TileCoordinate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileChangeLogTest {
    private final List<String> published = new ArrayList<>();
    private final List<Runnable> scheduled = new ArrayList<>();

    @Test
    void listsEachTileOnceAtItsLatestWrite() {
        final TileChangeLog log = this.log(8);
        log.record(new TileCoordinate(3, 0, 0));
        log.record(new TileCoordinate(2, -1, 4));
        log.record(new TileCoordinate(3, 0, 0));
        log.publish();
        final JsonObject json = this.lastPublished();
        assertEquals(3, json.get("seq").getAsLong());
        assertEquals(List.of(List.of(2L, 2L, -1L, 4L), List.of(3L, 3L, 0L, 0L)), changes(json));
    }

    @Test
    void evictionReportsTheNewestDroppedSequence() {
        final TileChangeLog log = this.log(2);
        for (int x = 0; x < 4; x++) {
            log.record(new TileCoordinate(0, x, 0));
        }
        log.publish();
        final JsonObject json = this.lastPublished();
        assertEquals(2, json.get("dropped").getAsLong());
        assertEquals(List.of(List.of(3L, 0L, 2L, 0L), List.of(4L, 0L, 3L, 0L)), changes(json));
    }

    @Test
    void recordingSchedulesOnePublishForPendingChanges() {
        final TileChangeLog log = this.log(8);
        log.record(new TileCoordinate(0, 1, 2));
        log.record(new TileCoordinate(0, 2, 2));
        assertEquals(1, this.scheduled.size());
        this.scheduled.getFirst().run();
        assertEquals(2, this.lastPublished().get("seq").getAsLong());
    }

    @Test
    void closePublishesPendingChangesOnce() {
        final TileChangeLog log = this.log(8);
        log.record(new TileCoordinate(0, 0, 0));
        log.close();
        assertEquals(1, this.lastPublished().get("seq").getAsLong());
        final int publishes = this.published.size();
        this.scheduled.getFirst().run();
        log.record(new TileCoordinate(0, 1, 0));
        log.close();
        assertEquals(1, this.scheduled.size());
        assertEquals(publishes, this.published.size());
    }

    private TileChangeLog log(final int maxEntries) {
        return new TileChangeLog(this.published::add, maxEntries, delay -> this.scheduled::add);
    }

    private JsonObject lastPublished() {
        return JsonParser.parseString(this.published.getLast()).getAsJsonObject();
    }

    private static List<List<Long>> changes(final JsonObject json) {
        final JsonArray changes = json.getAsJsonArray("changes");
        return changes.asList().stream()
            .map(change -> change.getAsJsonArray().asList().stream().map(element -> element.getAsLong()).toList())
            .toList();
    }
}
