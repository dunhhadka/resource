package org.example.order.order.application.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class SpreadsheetWriter {

    private final Writer writer;
    private final HashMap<String, Short> styles;
    private final Map<Integer, Pair<String, Map<Integer, String>>> rows;

    public SpreadsheetWriter(Writer writer, HashMap<String, Short> styles) {
        this.writer = writer;
        this.styles = styles;
        this.rows = new HashMap<>();
    }

    public void beginSheet(int baseColWidth, Map<Pair<Integer, Integer>, Integer> customColumWidth) throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"" +
                " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"" +
                " xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"" +
                " mc:Ignorable=\"x14ac xr xr2 xr3\"" +
                " xmlns:x14ac=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac\"" +
                " xmlns:xr=\"http://schemas.microsoft.com/office/spreadsheetml/2014/revision\"" +
                " xmlns:xr2=\"http://schemas.microsoft.com/office/spreadsheetml/2015/revision2\"" +
                " xmlns:xr3=\"http://schemas.microsoft.com/office/spreadsheetml/2016/revision3\"" +
                " xr:uid=\"{00000000-0001-0000-0000-000000000000}\">" +
                "<sheetViews><sheetView tabSelected=\"1\" workbookViewId=\"0\">" +
                "</sheetView></sheetViews>" +
                "<sheetFormatPr baseColWidth=\"" + baseColWidth +
                "\" defaultRowHeight=\"16\" x14ac:dyDescent=\"0.2\"/>");

        if (!customColumWidth.isEmpty()) {
            writer.write("<cols>");
            for (var entry : customColumWidth.entrySet()) {
                // write to column include : min , max, default-width
                writer.write("<col min=");
            }
            writer.write("</col>");
        }

        writer.write("<sheetData>");
    }

    public void addRow(int rowNum) {
        var row = rows.get(rowNum);
        if (row != null) return;
        row = createRow(rowNum);
        this.rows.put(rowNum, row);
    }

    private Pair<String, Map<Integer, String>> createRow(int rowNum) {
        var rowIndex = String.format(" r=\"%s\" x14ac:dyDescent=\"0.2\" ", rowNum);
        var rowHeight = ""; // create row height
        var rowAttributes = rowIndex + rowHeight;
        return Pair.of(rowAttributes, new HashMap<>());
    }

    public void writeValues(int rowNum, int startColNum, Object... values) {
        for (var value : values) {
            writeValue(rowNum, startColNum++, value);
        }
    }

    private void writeValue(int rowNum, int i, Object value) {
        var row = this.rows.get(rowNum);
        if (row == null) return;
        var valueInfo = handleValue(value);
    }

    private Object handleValue(Object value) {
        if (value == null) return null;
        return null;
    }

    /**
     * @b boolean
     * @d date in ISO8601 format
     * @e error
     * @inlineStr string that doesn't use the shared string table
     * @n number
     * @s shared string
     * @str formula string
     */
    public enum CellDataType {
        b,
        d,
        e,
        inlineStr,
        n,
        s,
        str
    }

}
