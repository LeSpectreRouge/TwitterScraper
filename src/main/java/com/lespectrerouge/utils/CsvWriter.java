package com.lespectrerouge.utils;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Writes values as rows in a comma-separated values (CSV) file.
 */
public final class CsvWriter implements Closeable {

    private final BufferedWriter writer;

    private final Map<Character, String> escapeMap = Map.of(
        '"', "\"\"",
        '\r', "",
        '\n', " "
    );

    /**
     * Creates a CSV writer and writes the header row.
     *
     * @param file the file to write to
     * @param header the CSV header row
     * @throws IOException if the file cannot be opened or the header cannot be written
     */
    public CsvWriter(File file, String header) throws IOException {
        writer = new BufferedWriter(new FileWriter(file));
        writer.write(header);
        writer.newLine();
    }

    /**
     * Writes a row containing the supplied values.
     *
     * @param values the values to write
     * @throws IOException if the row cannot be written
     */
    public void write(Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) writer.write(",");
            writer.write(escape(values[i]));
        }
        writer.newLine();
        writer.flush();
    }

    /**
     * Escapes a value according to the CSV format.
     *
     * @param value the value to escape
     * @return the escaped value enclosed in double quotes
     */
    private String escape(Object value) {
        if (value == null) return "\"\"";
        String text = String.valueOf(value);
        StringBuilder result = new StringBuilder(text.length() + 2);
        result.append('"');
        for (char c : text.toCharArray()) result.append(escapeMap.getOrDefault(c, String.valueOf(c)));
        return result.append('"').toString();
    }

    /**
     * Closes the underlying writer.
     *
     * @throws IOException if the writer cannot be closed
     */
    @Override
    public void close() throws IOException { writer.close(); }

}