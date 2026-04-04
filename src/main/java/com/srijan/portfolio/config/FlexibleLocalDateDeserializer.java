package com.srijan.portfolio.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)
    );

    private static final List<DateTimeFormatter> YEAR_MONTH_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("uuuu-MM", Locale.ENGLISH)
    );

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null) {
            return null;
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : YEAR_MONTH_FORMATTERS) {
            try {
                return YearMonth.parse(value, formatter).atDay(1);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return Year.parse(value, DateTimeFormatter.ofPattern("uuuu", Locale.ENGLISH)).atDay(1);
        } catch (DateTimeParseException ignored) {
        }

        throw InvalidFormatException.from(parser, "Date must be a valid date", value, LocalDate.class);
    }
}
