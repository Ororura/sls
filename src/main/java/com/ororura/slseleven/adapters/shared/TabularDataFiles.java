package com.ororura.slseleven.adapters.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class TabularDataFiles {

    private TabularDataFiles() {}

    public static List<List<String>> readXlsxRows(
        File file,
        List<String> errors
    ) {
        try (
            FileInputStream input = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(input)
        ) {
            Sheet sheet = workbook.getNumberOfSheets() > 0
                ? workbook.getSheetAt(0)
                : null;
            if (sheet == null) {
                errors.add("XLSX файл не содержит листов.");
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();
            List<List<String>> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                int lastCell = row.getLastCellNum();
                if (lastCell <= 0) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                boolean hasContent = false;
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    String value = cell == null
                        ? ""
                        : formatter.formatCellValue(cell);
                    if (!value.isBlank()) {
                        hasContent = true;
                    }
                    values.add(value.trim());
                }
                if (hasContent) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (Exception ex) {
            errors.add("Не удалось прочитать XLSX: " + ex.getMessage());
            return List.of();
        }
    }

    public static void loadDelimitedText(Window owner, String title, javafx.scene.control.TextArea target)
        throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter("TSV (*.tsv)", "*.tsv"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*")
            );
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        target.setText(readTextWithFallbackEncoding(file.toPath()));
    }

    public static String readTextWithFallbackEncoding(java.nio.file.Path path)
        throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.indexOf('\uFFFD') >= 0) {
            content = new String(bytes, Charset.forName("Windows-1251"));
        }
        return content;
    }
}
