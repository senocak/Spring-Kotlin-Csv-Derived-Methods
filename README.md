# Spring Data CSV

A Spring Boot starter that lets you use a Spring Data-style repository interface on top of CSV files.

The core module scans repository interfaces annotated with `@CsvFile`, creates proxy implementations at startup, loads CSV rows into memory for each repository call, applies CRUD or derived-query logic, and writes the full CSV back to disk for save/delete operations.

## Features

- **Spring Data-style repositories**: Define interfaces extending `CsvRepository<T, ID>`
- **Derived query methods**: Use method names like `findByName`, `findByPriceLessThan`, and `findByNameContaining`
- **Basic CRUD operations**: `save`, `saveAll`, `findById`, `findAll`, `delete`, `deleteById`, `existsById`, and `count`
- **Classpath seed files**: Package an initial CSV in `src/main/resources`
- **Writable runtime copies**: Mutating a classpath CSV writes to a real filesystem file
- **Jackson CSV mapping**: Entity fields are mapped from CSV headers

## Usage

Install or publish the core module as:

```xml
<dependency>
    <groupId>com.github.senocak</groupId>
    <artifactId>spring-data-csv</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Create Entity

Create a POJO and annotate it with `@CsvEntity`. CSV headers should match the entity property names.

```kotlin
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.senocak.csv.core.annotation.CsvEntity

@CsvEntity
data class Product(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("name")
    var name: String,
    @JsonProperty("price")
    var price: Double
)
```

### 2. Create Repository

Create a repository interface extending `CsvRepository` and annotate it with `@CsvFile`.

```java
import com.github.senocak.csv.core.annotation.CsvFile;
import com.github.senocak.csv.core.repository.CsvRepository;

import java.util.List;
import java.util.Optional;

@CsvFile(path = "classpath:users.csv")
public interface UserRepository extends CsvRepository<User, Long> {
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    List<User> findByNameAndAgeGreaterThan(String name, Integer age);
}
```

### 3. Enable CSV Repositories

Enable repository scanning in your Spring Boot application.

```java
import com.github.senocak.csv.core.annotation.EnableCsvRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCsvRepositories(basePackages = "com.example")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

If `basePackages` is not set, scanning starts from the package of the class annotated with `@EnableCsvRepositories`.

### 4. Add CSV Data

For a classpath CSV, place the file under `src/main/resources`.

```csv
id,name,age,email
1,John Doe,30,john@example.com
2,Jane Smith,25,jane@example.com
```

### 5. Use Repository

```java
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void example() {
        Iterable<User> allUsers = userRepository.findAll();

        List<User> usersNamedJohn = userRepository.findByName("John");

        User newUser = new User();
        newUser.setId(3L);
        newUser.setName("Jane");
        newUser.setAge(30);
        newUser.setEmail("jane.new@example.com");
        userRepository.save(newUser);

        userRepository.deleteById(1L);
    }
}
```

## How It Works

At startup, `@EnableCsvRepositories` imports the auto-configuration that scans for interfaces annotated with `@CsvFile`. For each repository interface, the library resolves the entity type and ID type from `CsvRepository<T, ID>`, then registers a proxy bean.

When a repository method is called:

1. `findAll`, `findById`, `existsById`, and `count` load the CSV into memory.
2. Derived query methods beginning with `find`, `get`, or `read` parse the method name using Spring Data Commons `PartTree`.
3. The proxy filters the in-memory entities using reflection.
4. `save`, `saveAll`, `delete`, and `deleteById` load all rows, modify the list in memory, then rewrite the whole CSV file.

This design keeps the library small and predictable. It is best suited for small configuration datasets, demos, fixtures, prototypes, and simple file-backed applications.

## Path Configuration

The `@CsvFile` annotation supports three path styles.

```java
@CsvFile(path = "classpath:users.csv")
@CsvFile(path = "data/users.csv")
@CsvFile(path = "/absolute/path/to/users.csv")
```

### Classpath Paths

`classpath:` paths are treated as seed data.

For `@CsvFile(path = "classpath:users.csv")`:

1. Before any write, reads come from the packaged classpath resource, for example `src/main/resources/users.csv`.
2. On `save`, `saveAll`, `delete`, or `deleteById`, the library writes a runtime copy to `System.getProperty("user.dir") + "/users.csv"`.
3. After that runtime file exists, all reads use the runtime copy instead of the classpath resource.

For nested classpath resources, the relative path is preserved. `classpath:data/users.csv` writes to:

```text
<working-directory>/data/users.csv
```

This is needed because classpath resources are often inside `target/classes` or inside a packaged JAR. Those locations are build artifacts, and in a packaged application they may not be writable at all. The classpath file is therefore a reliable initial seed, while the runtime filesystem copy is the mutable data file.

In the demo module, if the app runs with `csv-demo` as the working directory and uses:

```kotlin
@CsvFile(path = "classpath:products.csv")
```

the first read loads `csv-demo/src/main/resources/products.csv` through the classpath. The first save/delete writes `csv-demo/products.csv`, and later reads use `csv-demo/products.csv`.

### Relative And Absolute Filesystem Paths

Relative paths are resolved against the JVM working directory:

```java
@CsvFile(path = "data/users.csv")
```

This reads and writes:

```text
<working-directory>/data/users.csv
```

Absolute paths are used directly:

```java
@CsvFile(path = "/var/app/data/users.csv")
```

For mutable application data, a filesystem path is usually clearer than `classpath:` because reads and writes always target the same visible file from the first call.

## Supported Repository Operations

`CsvRepository<T, ID>` provides:

- `save(entity)`: appends the entity to the loaded rows and rewrites the CSV
- `saveAll(entities)`: appends all provided entities and rewrites the CSV
- `findById(id)`: finds the first entity whose ID field equals the provided ID
- `findAll()`: returns all rows from the active CSV file
- `delete(entity)`: removes equal entities and rewrites the CSV
- `deleteById(id)`: removes rows whose ID field equals the provided ID and rewrites the CSV
- `existsById(id)`: checks whether `findById` returns a value
- `count()`: returns the number of loaded rows

The ID lookup checks common field names: `id`, `Id`, `ID`, and `_id`.

`save` does not currently perform an upsert by ID. If you save another entity with the same ID, it is appended as another row.

## Supported Derived Queries

Derived methods are supported when the method name starts with `find`, `get`, or `read`.

Supported comparisons include:

- Equality: `findByName`, `findByEmail`
- String matching: `findByNameContaining`, `findByNameStartingWith`, `findByNameEndingWith`
- Numeric and comparable comparisons: `findByAgeGreaterThan`, `findByAgeGreaterThanEqual`, `findByAgeLessThan`, `findByAgeLessThanEqual`
- AND combinations: `findByNameAndAgeGreaterThan`

Supported return types include:

- `Optional<T>`
- `List<T>`, `Collection<T>`, or `Iterable<T>`
- A single entity type
- `long` or `Long` for matching row count
- `boolean` or `Boolean` for whether any row matched

## CSV File Format

The CSV file must include a header row. Header names should match entity fields or Jackson property names.

```csv
id,name,age,email
1,John Doe,30,john@example.com
2,Jane Smith,25,jane@example.com
```

Writes use Jackson's schema for the entity class and include a header row.

## Limitations

- All rows are loaded into memory on each repository call.
- Save and delete operations rewrite the whole CSV file.
- Writes are not transactional and no file locking is implemented.
- `save` appends rows; it does not update an existing row by ID.
- Derived OR queries are flattened by the current predicate builder and should not be relied on.
- `Between`, sorting, paging, and custom `@Query` methods are not implemented.
- Classpath CSV files should be treated as seed files, not the final mutable storage location.

## Demo

The demo application is in `csv-demo`.

```bash
mvn -f csv-core/pom.xml install
mvn -f csv-demo/pom.xml spring-boot:run
```