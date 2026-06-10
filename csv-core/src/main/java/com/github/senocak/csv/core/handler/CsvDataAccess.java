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
    private CsvDataAccess() {}

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
    public static <T> void saveAll(final String csvPath, final @NonNull List<?> entities, final Class<?> entityClass) throws IOException {
        final CsvSchema schema = csvMapper.schemaFor((Class<?>) entityClass).withHeader();
        final File file = getFile(csvPath);

        ensureParentDirectoryExists(file);

        csvMapper.writer(schema).writeValue(file, entities);
    }

    /**
     * Creates a MappingIterator for reading CSV data.
     */
    private static <T> MappingIterator<T> createIterator(final @NonNull String csvPath, final Class<T> entityClass, final CsvSchema schema) throws IOException {
        if (csvPath.startsWith("classpath:")) {
            final File writableFile = getFile(csvPath);
            if (writableFile.exists()) {
                return csvMapper.readerFor(entityClass).with(schema).readValues(writableFile);
            }
            final String resourcePath = getClasspathResourcePath(csvPath);
            final Resource resource = new ClassPathResource(resourcePath);
            final InputStream inputStream = resource.getInputStream();
            return csvMapper.readerFor(entityClass).with(schema).readValues(inputStream);
        }
        final File file = getFile(csvPath);
        if (!file.exists()) {
            // Create empty file if it doesn't exist
            ensureParentDirectoryExists(file);
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
            return new File(System.getProperty("user.dir"), getClasspathResourcePath(csvPath));
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
            return getFile(csvPath).getAbsolutePath();
        }
        return csvPath;
    }

    @NonNull
    private static String getClasspathResourcePath(final @NonNull String csvPath) {
        String resourcePath = csvPath.substring("classpath:".length());
        while (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        return resourcePath;
    }

    private static void ensureParentDirectoryExists(final @NonNull File file) throws IOException {
        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs() && !parentFile.exists()) {
            throw new IOException("Could not create directory: " + parentFile);
        }
    }
}
