// src/main/java/com/kmu/syncpos/util/OffsetDateTimeAdapter.java
package com.kmu.syncpos.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A GSON TypeAdapter for serializing and deserializing java.time.OffsetDateTime.
 * Converts OffsetDateTime to an ISO 8601 string and back.
 */
public class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {

    // The default formatter for OffsetDateTime already handles the format from Supabase
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(FORMATTER));
        }
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return OffsetDateTime.parse(in.nextString(), FORMATTER);
    }
}