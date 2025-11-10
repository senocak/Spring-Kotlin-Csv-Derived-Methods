package com.github.senocak.csv.controller;

import com.github.senocak.csv.entity.Product;
import com.github.senocak.csv.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;
    
    public ProductController(final ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @GetMapping
    public Iterable<Product> findAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Product> findById(@PathVariable("id") Long id) {
        return productRepository.findById(id);
    }

    @GetMapping("/search/by-name/{name}")
    public List<Product> findByName(@PathVariable("name") String name) {
        return productRepository.findByName(name);
    }

    @GetMapping("/search/by-name-containing/{name}")
    public List<Product> findByNameContaining(@PathVariable("name") String name) {
        return productRepository.findByNameContaining(name);
    }

    @GetMapping("/search/by-category/{category}")
    public List<Product> findByCategory(@PathVariable("category") String category) {
        return productRepository.findByCategory(category);
    }

    @GetMapping("/search/price-greater-than/{price}")
    public List<Product> findByPriceGreaterThan(@PathVariable("price") Double price) {
        return productRepository.findByPriceGreaterThan(price);
    }

    @GetMapping("/search/price-less-than/{price}")
    public List<Product> findByPriceLessThan(@PathVariable("price") Double price) {
        return productRepository.findByPriceLessThan(price);
    }

    @GetMapping("/search/stock-greater-than/{stock}")
    public List<Product> findByStockGreaterThan(@PathVariable("stock") Integer stock) {
        return productRepository.findByStockGreaterThan(stock);
    }

    @GetMapping("/search/stock-less-than/{category}/{price}")
    public List<Product> findByCategoryAndPriceLessThan(@PathVariable("category") String category, @PathVariable("price") Double price) {
        return productRepository.findByCategoryAndPriceLessThan(category, price);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product save(@RequestBody Product product) {
        return productRepository.save(product);
    }
    
    /**
     * Delete product by ID.
     */
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable("id") Long id) {
        productRepository.deleteById(id);
    }
}

