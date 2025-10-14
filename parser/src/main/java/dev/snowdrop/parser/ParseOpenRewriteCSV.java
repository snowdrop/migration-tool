package dev.snowdrop.parser;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParseOpenRewriteCSV {
    public static void main(String[] args) throws Exception {

        String DATA_TABLES = "/Users/cmoullia/code/application-modernisation/migration-tool-parent/applications/demo-spring-boot-todo-app/target/rewrite/datatables";

        List<Path> allFiles = new ArrayList<>();
        listAllFiles(Path.of(DATA_TABLES), allFiles);
        System.out.println("Found files:");
        allFiles.forEach(System.out::println);

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        allFiles.forEach(file -> {
            System.out.println("================================================");
            System.out.println("=== File processed : " + file.getFileName());

            // Parse CSV into generic Map structure instead of specific classes
            ObjectReader oReader = csvMapper.readerFor(Map.class).with(schema);

            // Pre-process CSV to remove the description row (line 2)
            List<String> lines;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file.toFile()))) {
                lines = bufferedReader.lines().collect(Collectors.toList());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // Remove the description row (index 1, which is line 2)
            if (lines.size() > 1) {
                lines.remove(1);
            }

            // Create cleaned CSV content
            String cleanedCsv = String.join("\n", lines);

            try (Reader reader = new StringReader(cleanedCsv)) {
                MappingIterator<Map<String, Object>> mi = oReader.readValues(reader);

                while (mi.hasNext()) {
                    Map<String, Object> current = mi.next();
                    System.out.println(current);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void listAllFiles(Path currentPath, List<Path> allFiles)
        throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listAllFiles(entry, allFiles);
                } else {
                    allFiles.add(entry);
                }
            }
        }
    }
}
