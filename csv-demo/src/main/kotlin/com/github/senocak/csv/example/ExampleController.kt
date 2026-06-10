package com.github.senocak.csv.example

import com.github.senocak.csv.core.annotation.EnableCsvRepositories
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.Optional

@RestController
@EnableCsvRepositories(basePackages = ["com.github.senocak"])
@RequestMapping(value = ["/api/products"])
class ExampleController(private val productRepository: ProductRepository) {
    @GetMapping
    fun findAll(): Iterable<Product> =
        productRepository.findAll()

    @GetMapping(value = ["/{id}"])
    fun findById(@PathVariable id: Long?): Optional<Product> =
        productRepository.findById(id)

    @GetMapping(value = ["/search/by-name/{name}"])
    fun findByName(@PathVariable name: String): MutableList<Product> =
        productRepository.findByName(name = name)

    @GetMapping(value = ["/search/by-name-containing/{name}"])
    fun findByNameContaining(@PathVariable name: String): MutableList<Product> =
        productRepository.findByNameContaining(name = name)

    @GetMapping(value = ["/search/by-category/{category}"])
    fun findByCategory(@PathVariable category: String): MutableList<Product> =
        productRepository.findByCategory(category = category)

    @GetMapping(value = ["/search/price-greater-than/{price}"])
    fun findByPriceGreaterThan(@PathVariable price: Double): MutableList<Product> =
        productRepository.findByPriceGreaterThan(price = price)

    @GetMapping(value = ["/search/price-less-than/{price}"])
    fun findByPriceLessThan(@PathVariable price: Double): MutableList<Product> =
        productRepository.findByPriceLessThan(price = price)

    @GetMapping(value = ["/search/stock-greater-than/{stock}"])
    fun findByStockGreaterThan(@PathVariable stock: Int): MutableList<Product> =
        productRepository.findByStockGreaterThan(stock = stock)

    @GetMapping(value = ["/search/stock-less-than/{category}/{price}"])
    fun findByCategoryAndPriceLessThan(
        @PathVariable category: String,
        @PathVariable price: Double
    ): MutableList<Product> = productRepository.findByCategoryAndPriceLessThan(category = category, price = price)

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    fun save(@RequestBody product: Product): Product =
        productRepository.save(product)

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long) {
        productRepository.deleteById(id)
    }
}
