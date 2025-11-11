package com.github.senocak.csv.core.annotation;

import com.github.senocak.csv.core.config.CsvRepositoryAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable CSV repositories.
 * Scans for interfaces annotated with @CsvFile and creates repository proxies.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(CsvRepositoryAutoConfiguration.class)
public @interface EnableCsvRepositories {
    /**
     * Base packages to scan for CSV repositories.
     * If not specified, scans the package of the annotated class.
     */
    String[] basePackages() default {};

    /**
     * Base package classes to scan for CSV repositories.
     * If not specified, scans the package of the annotated class.
     */
    Class<?>[] basePackageClasses() default {};
}

