package com.github.senocak.csv.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

/**
 * Invocation handler that intercepts method calls to CSV repositories
 * and executes them in-memory against CSV data.
 */
public class CsvRepositoryInvocationHandler implements java.lang.reflect.InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(CsvRepositoryInvocationHandler.class);
    private final Class<?> repositoryInterface;
    private final Class<?> entityClass;
    private final String csvPath;
    private final Class<?> idClass;

    public CsvRepositoryInvocationHandler(Class<?> repositoryInterface, Class<?> entityClass, String csvPath, Class<?> idClass) {
        this.repositoryInterface = repositoryInterface;
        this.entityClass = entityClass;
        this.csvPath = csvPath;
        this.idClass = idClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        final String methodName = method.getName();
        // Handle CRUD methods
        if (methodName.equals("findAll"))
            return findAll();
        if (methodName.equals("findById"))
            return findById(args[0]);
        if (methodName.equals("save"))
            return save(args[0]);
        if (methodName.equals("saveAll"))
            return saveAll((Iterable<?>) args[0]);
        if (methodName.equals("delete")) {
            delete(args[0]);
            return null;
        }
        if (methodName.equals("deleteById")) {
            deleteById(args[0]);
            return null;
        }
        if (methodName.equals("existsById"))
            return existsById(args[0]);
        if (methodName.equals("count"))
            return count();

        // Handle derived query methods (e.g., findByName, findByNameAndAgeGreaterThan)
        if (methodName.startsWith("find") || methodName.startsWith("get") || methodName.startsWith("read")) {
            return executeDerivedQuery(method, args);
        }

        throw new UnsupportedOperationException("Method " + methodName + " is not supported");
    }

    private List<?> findAll() {
        try {
            return CsvDataAccess.loadAll(csvPath, entityClass);
        } catch (Exception e) {
            throw new RuntimeException("Error loading CSV data", e);
        }
    }

    private Optional<?> findById(Object id) {
        try {
            return findAll().stream()
                    .filter(entity -> getIdValue(entity).equals(id))
                    .findFirst();
        } catch (Exception e) {
            throw new RuntimeException("Error finding entity by id", e);
        }
    }

    private Object save(Object entity) {
        try {
            final List<Object> all = new ArrayList<>(findAll());
            all.add(entity);
            CsvDataAccess.saveAll(CsvDataAccess.getWritablePath(csvPath), all, (Class<Object>) entityClass);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error saving entity", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Iterable<?> saveAll(Iterable<?> entities) {
        try {
            final List<Object> all = new ArrayList<>(findAll());
            entities.forEach(all::add);
            CsvDataAccess.saveAll(CsvDataAccess.getWritablePath(csvPath), all, (Class<Object>) entityClass);
            return entities;
        } catch (Exception e) {
            throw new RuntimeException("Error saving entities", e);
        }
    }

    private void delete(Object entity) {
        try {
            final List<Object> all = new ArrayList<>(findAll());
            all.removeIf(e -> e.equals(entity));
            CsvDataAccess.saveAll(CsvDataAccess.getWritablePath(csvPath), all, (Class<Object>) entityClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting entity", e);
        }
    }

    private void deleteById(Object id) {
        try {
            final List<Object> all = new ArrayList<>(findAll());
            all.removeIf(entity -> getIdValue(entity).equals(id));
            CsvDataAccess.saveAll(CsvDataAccess.getWritablePath(csvPath), all, (Class<Object>) entityClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting entity by id", e);
        }
    }

    private boolean existsById(Object id) {
        return findById(id).isPresent();
    }

    private long count() {
        return findAll().size();
    }

    /**
     * Executes a derived query method by parsing the method name and filtering in-memory.
     */
    private Object executeDerivedQuery(final Method method, final Object[] args) {
        final List<?> all = findAll();
        final String name = method.getName();
        if (name.equals("findAll")) {
            return all;
        }
        final PartTree partTree = new PartTree(name, entityClass);
        final Predicate<Object> predicate = buildPredicate(partTree, args);
        final List<?> result = all.stream().filter(predicate).toList();
        final Class<?> returnType = method.getReturnType();
        if (returnType == Optional.class) {
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } else if (returnType == List.class || returnType == Iterable.class || returnType == Collection.class) {
            return result;
        } else if (returnType.isAssignableFrom(entityClass)) {
            return result.isEmpty() ? null : result.get(0);
        } else if (returnType == long.class || returnType == Long.class) {
            return (long) result.size();
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return !result.isEmpty();
        }
        return result;
    }

    /**
     * Builds a Predicate from a PartTree and method arguments.
     * Uses reflection to access PartTree's internal structure.
     */
    private Predicate<Object> buildPredicate(PartTree partTree, Object[] args) {
        final List<Part> parts = partTree.getParts().toList();
        int argIndex = 0;
        final List<Predicate<Object>> predicates = new ArrayList<>();
        for (final Part part : parts) {
            final String propertyName = part.getProperty().toDotPath();
            final Enum<?> type = part.getType();
            if (argIndex >= args.length) {
                break;
            }
            final Object argValue = args[argIndex++];
            final Predicate<Object> partPredicate = createPredicate(propertyName, type, argValue);
            predicates.add(partPredicate);
        }

        // Combine all predicates with AND
        return predicates.stream()
                .reduce(Predicate::and)
                .orElse(entity -> true);
    }

    /**
     * Creates a Predicate for a single property comparison.
     */
    private Predicate<Object> createPredicate(String propertyName, Enum<?> type, Object value) {
        return entity -> {
            try {
                Object propertyValue = getPropertyValue(entity, propertyName);
                if (propertyValue == null) {
                    return false;
                }
                final String typeName = type.name();
                if ("EQUALS".equals(typeName) || "IS".equals(typeName) || "SIMPLE_PROPERTY".equals(typeName)) {
                    return propertyValue.equals(value);
                } else if ("NOT_EQUALS".equals(typeName) || "IS_NOT".equals(typeName)) {
                    return !propertyValue.equals(value);
                } else if ("CONTAINING".equals(typeName) || "LIKE".equals(typeName)) {
                    return propertyValue.toString().contains(value.toString());
                } else if ("NOT_CONTAINING".equals(typeName) || "NOT_LIKE".equals(typeName)) {
                    return !propertyValue.toString().contains(value.toString());
                } else if ("STARTING_WITH".equals(typeName)) {
                    return propertyValue.toString().startsWith(value.toString());
                } else if ("ENDING_WITH".equals(typeName)) {
                    return propertyValue.toString().endsWith(value.toString());
                } else if (propertyValue instanceof Comparable && value instanceof Comparable) {
                    if (propertyValue instanceof Number propertyNumber && value instanceof Number valueNumber) {
                        final double propertyNum = propertyNumber.doubleValue();
                        final double valueNum = valueNumber.doubleValue();
                        return switch (typeName) {
                            case "GREATER_THAN" -> propertyNum > valueNum;
                            case "GREATER_THAN_EQUAL" -> propertyNum >= valueNum;
                            case "LESS_THAN" -> propertyNum < valueNum;
                            case "LESS_THAN_EQUAL" -> propertyNum <= valueNum;
                            default -> false;
                        };
                    }
                    final Comparable<Object> comparable = (Comparable<Object>) propertyValue;
                    final Comparable<Object> comparableValue = (Comparable<Object>) value;
                    final int comparison = comparable.compareTo(comparableValue);
                    return switch (typeName) {
                        case "GREATER_THAN" -> comparison > 0;
                        case "GREATER_THAN_EQUAL" -> comparison >= 0;
                        case "LESS_THAN" -> comparison < 0;
                        case "LESS_THAN_EQUAL" -> comparison <= 0;
                        default -> false;
                    };
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Gets the value of a property from an entity using reflection.
     */
    private Object getPropertyValue(Object entity, String propertyPath) {
        try {
            String[] parts = propertyPath.split("\\.");
            Object current = entity;

            for (String part : parts) {
                Field field = findField(current.getClass(), part);
                if (field == null) {
                    // Try getter method
                    String getterName = "get" + capitalize(part);
                    Method getter = findMethod(current.getClass(), getterName);
                    if (getter != null) {
                        current = getter.invoke(current);
                    } else {
                        return null;
                    }
                } else {
                    field.setAccessible(true);
                    current = field.get(current);
                }
                if (current == null) {
                    return null;
                }
            }
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the ID value from an entity.
     */
    private Object getIdValue(Object entity) {
        // Try common ID field names
        String[] idFieldNames = {"id", "Id", "ID", "_id"};
        for (String fieldName : idFieldNames) {
            try {
                Field field = findField(entity.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
                // Try getter
                String getterName = "get" + capitalize(fieldName);
                Method getter = findMethod(entity.getClass(), getterName);
                if (getter != null) {
                    return getter.invoke(entity);
                }
            } catch (Exception ignored) {
                log.error("Error accessing ID field", ignored);
            }
        }
        throw new RuntimeException("Could not find ID field in entity: " + entity.getClass().getName());
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

