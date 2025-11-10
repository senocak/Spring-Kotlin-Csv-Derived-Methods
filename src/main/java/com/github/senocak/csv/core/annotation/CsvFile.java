package com.github.senocak.csv.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the CSV file path for a repository.
 * Can be a classpath resource (e.g., "classpath:data.csv") or filesystem path.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CsvFile {
    /**
     * The path to the CSV file.
     * Supports classpath: prefix for resources or absolute/relative filesystem paths.
     */
    String path();
}

