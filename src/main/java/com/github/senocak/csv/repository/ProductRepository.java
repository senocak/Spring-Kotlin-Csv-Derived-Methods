package com.github.senocak.csv.repository;

import com.github.senocak.csv.core.annotation.CsvFile;
import com.github.senocak.csv.entity.Product;
import com.github.senocak.csv.core.repository.CsvRepository;
import java.util.List;

@CsvFile(path = "classpath:products.csv")
public interface ProductRepository extends CsvRepository<Product, Long> {
    List<Product> findByName(String name);
    List<Product> findByNameContaining(String name);
    List<Product> findByCategory(String category);
    List<Product> findByPriceGreaterThan(Double price);
    List<Product> findByPriceLessThan(Double price);
    List<Product> findByStockGreaterThan(Integer stock);
    List<Product> findByCategoryAndPriceLessThan(String category, Double price);
}

