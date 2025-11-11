package com.github.senocak.csv.core.handler;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading and writing CSV files.
 */
public class CsvDataAccess {

    private static final CsvMapper csvMapper = new CsvMapper();

    /**
     * Loads all entities from a CSV file.
     *
     * @param csvPath    The path to the CSV file
     * @param entityClass The entity class to map to
     * @param <T>        The entity type
     * @return List of entities
     * @throws IOException if the file cannot be read
     */
    public static <T> List<T> loadAll(String csvPath, Class<T> entityClass) throws IOException {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        MappingIterator<T> iterator = createIterator(csvPath, entityClass, schema);
        List<T> entities = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                entities.add(iterator.next());
            }
        } finally {
            iterator.close();
        }
        return entities;
    }

    /**
     * Saves all entities to a CSV file, overwriting the existing file.
     *
     * @param csvPath    The path to the CSV file
     * @param entities   The entities to save
     * @param entityClass The entity class
     * @param <T>        The entity type
     * @throws IOException if the file cannot be written
     */
    @SuppressWarnings("unchecked")
    public static <T> void saveAll(String csvPath, List<?> entities, Class<?> entityClass) throws IOException {
        if (entities.isEmpty()) {
            return;
        }

        CsvSchema schema = csvMapper.schemaFor((Class<?>) entityClass).withHeader();
        File file = getFile(csvPath);

        // Ensure parent directory exists
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        csvMapper.writer(schema).writeValue(file, entities);
    }

    /**
     * Creates a MappingIterator for reading CSV data.
     */
    private static <T> MappingIterator<T> createIterator(String csvPath, Class<T> entityClass, CsvSchema schema) throws IOException {
        if (csvPath.startsWith("classpath:")) {
            String resourcePath = csvPath.substring("classpath:".length());
            Resource resource = new ClassPathResource(resourcePath);
            InputStream inputStream = resource.getInputStream();
            return csvMapper.readerFor(entityClass).with(schema).readValues(inputStream);
        } else {
            File file = getFile(csvPath);
            if (!file.exists()) {
                // Create empty file if it doesn't exist
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            return csvMapper.readerFor(entityClass).with(schema).readValues(file);
        }
    }

    /**
     * Gets a File object from a path string.
     * Handles both absolute and relative paths.
     */
    private static File getFile(String csvPath) {
        if (csvPath.startsWith("classpath:")) {
            // For classpath resources, we need to write to a filesystem location
            // Default to writing to the current directory or a temp location
            String resourcePath = csvPath.substring("classpath:".length());
            String fileName = Paths.get(resourcePath).getFileName().toString();
            return new File(System.getProperty("user.dir"), fileName);
        } else {
            java.nio.file.Path path = Paths.get(csvPath);
            if (path.isAbsolute()) {
                return path.toFile();
            } else {
                return new File(System.getProperty("user.dir"), csvPath).getAbsoluteFile();
            }
        }
    }

    /**
     * Gets the actual file path for writing operations.
     * For classpath resources, returns a filesystem path.
     */
    public static String getWritablePath(String csvPath) {
        if (csvPath.startsWith("classpath:")) {
            String resourcePath = csvPath.substring("classpath:".length());
            String fileName = Paths.get(resourcePath).getFileName().toString();
            return new File(System.getProperty("user.dir"), fileName).getAbsolutePath();
        }
        return csvPath;
    }
}

