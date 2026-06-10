package com.github.senocak.csv.core.handler;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading and writing CSV files.
 */
public class CsvDataAccess {
    private static final Logger log = LoggerFactory.getLogger(CsvDataAccess.class);
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
    @NonNull
    public static <T> List<T> loadAll(final String csvPath, final Class<T> entityClass) throws IOException {
        final CsvSchema schema = CsvSchema.emptySchema().withHeader();
        final MappingIterator<T> iterator = createIterator(csvPath, entityClass, schema);
        final List<T> entities = new ArrayList<>();
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
    public static <T> void saveAll(final String csvPath, final @NonNull List<?> entities, final Class<?> entityClass) throws IOException {
        if (entities.isEmpty()) {
            return;
        }
        final CsvSchema schema = csvMapper.schemaFor((Class<?>) entityClass).withHeader();
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
    private static <T> MappingIterator<T> createIterator(final @NonNull String csvPath, final Class<T> entityClass, final CsvSchema schema) throws IOException {
        if (csvPath.startsWith("classpath:")) {
            final String resourcePath = csvPath.substring("classpath:".length());
            final Resource resource = new ClassPathResource(resourcePath);
            final InputStream inputStream = resource.getInputStream();
            return csvMapper.readerFor(entityClass).with(schema).readValues(inputStream);
        }
        final File file = getFile(csvPath);
        if (!file.exists()) {
            // Create empty file if it doesn't exist
            file.getParentFile().mkdirs();
            final boolean newFile = file.createNewFile();
            log.info("Creating new file {}", newFile);
        }
        return csvMapper.readerFor(entityClass).with(schema).readValues(file);
    }

    /**
     * Gets a File object from a path string.
     * Handles both absolute and relative paths.
     */
    private static File getFile(final @NonNull String csvPath) {
        if (csvPath.startsWith("classpath:")) {
            // For classpath resources, we need to write to a filesystem location
            // Default to writing to the current directory or a temp location
            final String resourcePath = csvPath.substring("classpath:".length());
            final String fileName = Paths.get(resourcePath).getFileName().toString();
            return new File(System.getProperty("user.dir"), fileName);
        }
        final Path path = Paths.get(csvPath);
        if (path.isAbsolute()) {
            return path.toFile();
        }
        return new File(System.getProperty("user.dir"), csvPath).getAbsoluteFile();
    }

    /**
     * Gets the actual file path for writing operations.
     * For classpath resources, returns a filesystem path.
     */
    @NonNull
    public static String getWritablePath(final @NonNull String csvPath) {
        if (csvPath.startsWith("classpath:")) {
            final String resourcePath = csvPath.substring("classpath:".length());
            final String fileName = Paths.get(resourcePath).getFileName().toString();
            return new File(System.getProperty("user.dir"), fileName).getAbsolutePath();
        }
        return csvPath;
    }
}
