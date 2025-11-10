# Spring Data CSV

A Spring Boot starter library that enables developers to use the Spring Data Repository pattern to interact with CSV files as if they were a database.

## Features

- **Spring Data Repository Pattern**: Use familiar Spring Data repository interfaces
- **Derived Query Methods**: Support for method name-based queries (e.g., `findByName`, `findByNameAndAgeGreaterThan`)
- **CRUD Operations**: Full support for Create, Read, Update, and Delete operations
- **In-Memory Query Execution**: All queries are executed in-memory against CSV data
- **Jackson CSV Integration**: Robust CSV parsing and POJO mapping using Jackson

## Technology Stack

- **Java 17**
- **Spring Boot 3**
- **Spring Data Commons**
- **Jackson CSV**

## Quick Start

### 1. Add Dependency

Add the library to your `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spring-data-csv</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Create Entity

Create a POJO class and annotate it with `@CsvEntity`:

```java
import annotation.com.github.senocak.csv.CsvEntity;

@CsvEntity
public class User {
    private Long id;
    private String name;
    private Integer age;
    private String email;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

### 3. Create Repository

Create a repository interface extending `CsvRepository` and annotate it with `@CsvFile`:

```java
import annotation.com.github.senocak.csv.CsvFile;
import repository.com.github.senocak.csv.CsvRepository;

@CsvFile(path = "classpath:users.csv")
public interface UserRepository extends CsvRepository<User, Long> {
    // Derived query methods
    List<User> findByName(String name);

    Optional<User> findByEmail(String email);

    List<User> findByNameAndAgeGreaterThan(String name, Integer age);

    List<User> findByAgeBetween(Integer minAge, Integer maxAge);
}
```

### 4. Enable CSV Repositories

Enable CSV repositories in your Spring Boot application:

```java
import annotation.com.github.senocak.csv.EnableCsvRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCsvRepositories
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 5. Use Repository

Inject and use the repository in your service:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void example() {
        // Find all users
        Iterable<User> allUsers = userRepository.findAll();

        // Find by name
        List<User> users = userRepository.findByName("John");

        // Find by name and age
        List<User> users = userRepository.findByNameAndAgeGreaterThan("John", 25);

        // Save a new user
        User newUser = new User();
        newUser.setName("Jane");
        newUser.setAge(30);
        userRepository.save(newUser);

        // Delete a user
        userRepository.deleteById(1L);
    }
}
```

## Supported Query Methods

The library supports the following derived query method patterns:

- **Equality**: `findByName`, `findByEmail`
- **Comparison**: `findByAgeGreaterThan`, `findByAgeLessThan`, `findByAgeBetween`
- **String Matching**: `findByNameContaining`, `findByNameStartingWith`, `findByNameEndingWith`
- **Combinations**: `findByNameAndAge`, `findByNameOrEmail`

## CSV File Format

The CSV file should have a header row with column names matching the entity field names:

```csv
id,name,age,email
1,John Doe,30,john@example.com
2,Jane Smith,25,jane@example.com
```

## Path Configuration

The `@CsvFile` annotation supports:

- **Classpath resources**: `@CsvFile(path = "classpath:data.csv")`
- **Filesystem paths**: `@CsvFile(path = "/path/to/data.csv")`
- **Relative paths**: `@CsvFile(path = "data/users.csv")`

## Limitations

- All queries are executed in-memory, so performance may degrade with very large CSV files
- Write operations overwrite the entire CSV file
- Complex queries (OR conditions) are simplified to handle the first OR part only

## License

This project is licensed under the MIT License.

