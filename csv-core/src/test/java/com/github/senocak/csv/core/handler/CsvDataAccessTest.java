package com.github.senocak.csv.core.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvDataAccessTest {

    @TempDir
    Path tempDir;

    @Test
    void classpathResourceReadsWritableCopyAfterSave() throws Exception {
        final String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            final List<Product> seededProducts = CsvDataAccess.loadAll("classpath:seed-products.csv", Product.class);
            assertThat(seededProducts).extracting(Product::getName).containsExactly("Seed");

            final Product savedProduct = new Product(2L, "Saved");
            CsvDataAccess.saveAll(
                    CsvDataAccess.getWritablePath("classpath:seed-products.csv"),
                    List.of(savedProduct),
                    Product.class
            );

            final List<Product> reloadedProducts = CsvDataAccess.loadAll("classpath:seed-products.csv", Product.class);

            assertThat(reloadedProducts).extracting(Product::getName).containsExactly("Saved");
            assertThat(Files.exists(tempDir.resolve("seed-products.csv"))).isTrue();
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    public static class Product {
        private Long id;
        private String name;

        public Product() {
        }

        public Product(final Long id, final String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
