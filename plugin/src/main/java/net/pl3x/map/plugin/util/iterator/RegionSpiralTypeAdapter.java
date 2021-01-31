package net.pl3x.map.plugin.util.iterator;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class RegionSpiralTypeAdapter extends TypeAdapter<RegionSpiralIterator> {
    @Override
    public RegionSpiralIterator read(JsonReader reader) throws IOException {
        RegionSpiralIterator spiral = new RegionSpiralIterator(0, 0, 0);
        reader.beginObject();
        String field = null;

        while (reader.hasNext()) {
            if (reader.peek().equals(JsonToken.NAME)) {
                field = reader.nextName();
            }
            if ("x".equals(field)) {
                spiral.x = reader.nextInt();
            }
            if ("z".equals(field)) {
                spiral.z = reader.nextInt();
            }
            if ("stepCount".equals(field)) {
                spiral.stepCount = reader.nextInt();
            }
            if ("stepLeg".equals(field)) {
                spiral.stepLeg = reader.nextInt();
            }
            if ("legAxis".equals(field)) {
                spiral.legAxis = reader.nextInt();
            }
            if ("layer".equals(field)) {
                spiral.layer = reader.nextInt();
            }
            if ("totalSteps".equals(field)) {
                spiral.totalSteps = reader.nextInt();
            }
            if ("direction".equals(field)) {
                spiral.direction = AbstractSpiralIterator.Direction.of(reader.nextInt());
            }
        }
        reader.endObject();
        return spiral;
    }

    @Override
    public void write(JsonWriter writer, RegionSpiralIterator spiral) throws IOException {
        writer.beginObject();
        writer.name("x");
        writer.value(spiral.x);
        writer.name("z");
        writer.value(spiral.z);
        writer.name("stepCount");
        writer.value(spiral.stepCount);
        writer.name("stepLeg");
        writer.value(spiral.stepLeg);
        writer.name("legAxis");
        writer.value(spiral.legAxis);
        writer.name("layer");
        writer.value(spiral.layer);
        writer.name("totalSteps");
        writer.value(spiral.totalSteps);
        writer.name("direction");
        writer.value(spiral.direction.ordinal());
        writer.endObject();
    }
}
