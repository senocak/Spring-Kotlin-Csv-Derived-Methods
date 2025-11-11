package com.github.senocak.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.senocak.csv.core.annotation.CsvEntity
import com.github.senocak.csv.core.annotation.CsvFile
import com.github.senocak.csv.core.annotation.EnableCsvRepositories
import com.github.senocak.csv.core.repository.CsvRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
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

fun main(args: Array<String>) {
	runApplication<ExampleApplication>(*args)
}

@CsvEntity
data class Product(
    @JsonProperty(value = "id")
    var id: Long,

    @JsonProperty(value = "name")
    var name: String,

    @JsonProperty(value = "description")
    var description: String,

    @JsonProperty(value = "price")
    var price: Double,

    @JsonProperty(value = "category")
    var category: String,

    @JsonProperty(value = "stock")
    var stock: Int
)

@CsvFile(path = "classpath:products.csv")
interface ProductRepository : CsvRepository<Product, Long> {
    fun findByName(name: String): MutableList<Product>
    fun findByNameContaining(name: String): MutableList<Product>
    fun findByCategory(category: String): MutableList<Product>
    fun findByPriceGreaterThan(price: Double): MutableList<Product>
    fun findByPriceLessThan(price: Double): MutableList<Product>
    fun findByStockGreaterThan(stock: Int): MutableList<Product>
    fun findByCategoryAndPriceLessThan(category: String, price: Double): MutableList<Product>
}

@SpringBootApplication
@RestController
@EnableCsvRepositories(basePackages = ["com.github.senocak"])
//@EnableCsvRepositories(basePackageClasses = [ProductRepository::class])
@RequestMapping(value = ["/api/products"])
class ExampleApplication(private val productRepository: ProductRepository) {
    @GetMapping
    fun findAll(): Iterable<Product> =
        productRepository.findAll()

    @GetMapping(value = ["/{id}"])
    fun findById(@PathVariable(value = "id") id: Long?): Optional<Product> =
        productRepository.findById(id)

    @GetMapping(value = ["/search/by-name/{name}"])
    fun findByName(@PathVariable(value = "name") name: String): MutableList<Product> =
        productRepository.findByName(name = name)

    @GetMapping(value = ["/search/by-name-containing/{name}"])
    fun findByNameContaining(@PathVariable(value = "name") name: String): MutableList<Product> =
        productRepository.findByNameContaining(name = name)

    @GetMapping(value = ["/search/by-category/{category}"])
    fun findByCategory(@PathVariable(value = "category") category: String): MutableList<Product> =
        productRepository.findByCategory(category = category)

    @GetMapping(value = ["/search/price-greater-than/{price}"])
    fun findByPriceGreaterThan(@PathVariable(value = "price") price: Double): MutableList<Product> =
        productRepository.findByPriceGreaterThan(price = price)

    @GetMapping(value = ["/search/price-less-than/{price}"])
    fun findByPriceLessThan(@PathVariable(value = "price") price: Double): MutableList<Product> =
        productRepository.findByPriceLessThan(price = price)

    @GetMapping(value = ["/search/stock-greater-than/{stock}"])
    fun findByStockGreaterThan(@PathVariable(value = "stock") stock: Int): MutableList<Product> =
        productRepository.findByStockGreaterThan(stock = stock)

    @GetMapping(value = ["/search/stock-less-than/{category}/{price}"])
    fun findByCategoryAndPriceLessThan(
        @PathVariable(value = "category") category: String,
        @PathVariable(value = "price") price: Double
    ): MutableList<Product> = productRepository.findByCategoryAndPriceLessThan(category = category, price = price)

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    fun save(@RequestBody product: Product): Product = productRepository.save(product)
    // TODO: save is not saving to CSV file. Needs to be fixed.

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable(value = "id") id: Long) {
        productRepository.deleteById(id)
    }
}
