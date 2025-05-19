package org.example.order.order.application.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class ExcelUtils {

    public static ExcelUtilModel genSpreadSheetWriter(String sheetName) {
        return genSpreadSheetWriter(RequestExcelFile.builder().sheetName(sheetName).build());
    }

    private static ExcelUtilModel genSpreadSheetWriter(RequestExcelFile requestExcelFile) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        var styleCustom = requestExcelFile.getStyles();
        var styleList = createStylesDefault(workbook);
        var styles = new HashMap<String, Short>();

        styleList.forEach((key, value) -> styles.put(key, value.getIndex()));

        if (styleCustom != null && !styleCustom.isEmpty()) {
            styleCustom.forEach((key, value) -> styles.put(key, value.getIndex()));
        }

        XSSFSheet sheet = workbook.createSheet(requestExcelFile.getSheetName());
        String randomUUID = UUID.randomUUID().toString();
        String templateFileName = String.format("template_%s.xlsx", randomUUID);

        List<SpreadsheetWriter> spreadsheetWriters = new ArrayList<>();
        List<File> fileWriters = new ArrayList<>();
        List<Writer> writers = new ArrayList<>();

        try (FileOutputStream fos = new FileOutputStream(templateFileName)) {

            workbook.write(fos);

            for (int i = 0; i < requestExcelFile.getSplitSpreadSheetWriter(); i++) {
                File tmpFile = File.createTempFile(String.format("sheet_%d_%s", i, randomUUID), ".xml");
                Writer writer = new OutputStreamWriter(Files.newOutputStream(tmpFile.toPath()), StandardCharsets.UTF_8);

                SpreadsheetWriter sw = new SpreadsheetWriter(writer, styles);
                if (i == 0) {
                    sw.beginSheet(requestExcelFile.getBaseColWidth(), requestExcelFile.getCustomColumWidth());
                }

                spreadsheetWriters.add(sw);
                fileWriters.add(tmpFile);
                writers.add(writer);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var excelUtilModel = new ExcelUtilModel();
        excelUtilModel.setSpreadsheetWriters(spreadsheetWriters);
        excelUtilModel.setFiles(fileWriters);
        excelUtilModel.setWriters(writers);
        excelUtilModel.setTemplateFile(templateFileName);

        return excelUtilModel;
    }

    private static Map<String, XSSFCellStyle> createStylesDefault(XSSFWorkbook workbook) {
        Map<String, XSSFCellStyle> styles = new HashMap<>();
        DataFormat df = workbook.createDataFormat();

        XSSFCellStyle normalTextNoBorder = workbook.createCellStyle();
        styles.put(StylesDefault.normalTextNoBorder.name, normalTextNoBorder);

        // Tạo những style chung sau đó đẩy vào map

        return styles;
    }


    @Getter
    @Setter
    public static class ExcelUtilModel {
        private List<SpreadsheetWriter> spreadsheetWriters;
        private List<File> files;
        private List<Writer> writers;

        private String templateFile;

        private Map<String, Short> styles;

        public List<org.springframework.data.util.Pair<String, String>> getMergedCells() {
            return null;
        }
    }

    @Getter
    @Builder
    public static class RequestExcelFile {
        @Builder.Default
        private String sheetName = "Sheet1";

        @Builder.Default
        private int splitSpreadSheetWriter = 1;

        @Builder.Default
        private Map<Pair<Integer, Integer>, Integer> customColumWidth = new HashMap<>();

        @Builder.Default
        private int baseColWidth = 12;

        private Map<String, XSSFCellStyle> styles;
    }

    @Getter
    public enum StylesDefault {

        normalTextNoBorder("normalTextNoBorder"),
        normalTextCenterNoBorder("normalTextCenterNoBorder"),
        normalTextCenterFullNoneBorder("normalTextCenterFullNoneBorder"),
        normalDateNoBorder("normalDateNoBorder"),
        headerText("headerText"),
        headerTable("headerTable"),
        normalText("normalText"),
        zeroNumber("zeroNumber"),
        normalCenterText("normalCenterText"),
        normalDate("normalDate"),
        headerTableNumber("headerTableNumber"),
        headerTableDecimal("headerTableDecimal"),
        normalNumber("normalNumber"),
        decimalNumber("decimalNumber");

        private final String name;

        StylesDefault(String name) {
            this.name = name;
        }

    }
}
