package com.github.senocak.csv.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.senocak.csv.core.annotation.CsvEntity
import com.github.senocak.csv.core.annotation.CsvFile
import com.github.senocak.csv.core.repository.CsvRepository

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

@CsvFile(path = "classpath:products.csv") // TODO: Make it configurable like SpEL
interface ProductRepository : CsvRepository<Product, Long> {
    fun findByName(name: String): MutableList<Product>
    fun findByNameContaining(name: String): MutableList<Product>
    fun findByCategory(category: String): MutableList<Product>
    fun findByPriceGreaterThan(price: Double): MutableList<Product>
    fun findByPriceLessThan(price: Double): MutableList<Product>
    fun findByStockGreaterThan(stock: Int): MutableList<Product>
    fun findByCategoryAndPriceLessThan(category: String, price: Double): MutableList<Product>
}
