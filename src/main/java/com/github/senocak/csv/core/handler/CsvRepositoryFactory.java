package com.github.senocak.csv.core.handler;

import com.github.senocak.csv.core.annotation.CsvFile;
import com.github.senocak.csv.core.annotation.CsvEntity;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.TypeInformation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Factory class for creating CSV repository proxies.
 */
public class CsvRepositoryFactory {

    /**
     * Creates a repository proxy for the given interface.
     */
    public static Object createProxy(Class<?> repositoryInterface) {
        RepositoryMetadata metadata = extractMetadata(repositoryInterface);
        SimpleRepositoryMetadata simpleMetadata = (SimpleRepositoryMetadata) metadata;
        
        CsvRepositoryInvocationHandler handler = new CsvRepositoryInvocationHandler(
                repositoryInterface,
                simpleMetadata.getEntityClass(),
                simpleMetadata.getCsvPath(),
                simpleMetadata.getIdClass());
        
        return Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class[]{repositoryInterface},
                handler);
    }

    /**
     * Extracts repository metadata from the interface.
     */
    private static RepositoryMetadata extractMetadata(Class<?> repositoryInterface) {
        // Extract entity class from repository interface
        Class<?> entityClass = null;
        Class<?> idClass = null;

        Type[] genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length >= 1) {
                    Type entityType = actualTypeArguments[0];
                    if (entityType instanceof Class) {
                        entityClass = (Class<?>) entityType;
                    }
                }
                if (actualTypeArguments.length >= 2) {
                    Type idType = actualTypeArguments[1];
                    if (idType instanceof Class) {
                        idClass = (Class<?>) idType;
                    }
                }
            }
        }

        // If not found, try to get from superclass
        if (entityClass == null) {
            Type superclass = repositoryInterface.getGenericSuperclass();
            if (superclass instanceof ParameterizedType parameterizedType) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length >= 1) {
                    Type entityType = actualTypeArguments[0];
                    if (entityType instanceof Class) {
                        entityClass = (Class<?>) entityType;
                    }
                }
                if (actualTypeArguments.length >= 2) {
                    Type idType = actualTypeArguments[1];
                    if (idType instanceof Class) {
                        idClass = (Class<?>) idType;
                    }
                }
            }
        }

        // Get CSV path from annotation
        CsvFile csvFileAnnotation = repositoryInterface.getAnnotation(CsvFile.class);
        String csvPath = null;
        if (csvFileAnnotation != null) {
            csvPath = csvFileAnnotation.path();
        }

        // Validate entity class has @CsvEntity annotation
        if (entityClass != null && !entityClass.isAnnotationPresent(CsvEntity.class)) {
            throw new IllegalArgumentException("Entity class " + entityClass.getName() + " must be annotated with @CsvEntity");
        }

        // Validate CSV path is specified
        if (csvPath == null || csvPath.isEmpty()) {
            throw new IllegalArgumentException("Repository " + repositoryInterface.getName() + " must be annotated with @CsvFile");
        }

        return new SimpleRepositoryMetadata(repositoryInterface, entityClass, idClass, csvPath);
    }

    /**
     * Simple implementation of RepositoryMetadata.
     */
    private static class SimpleRepositoryMetadata implements RepositoryMetadata {
        private final Class<?> repositoryInterface;
        private final Class<?> entityClass;
        private final Class<?> idClass;
        private final String csvPath;

        public SimpleRepositoryMetadata(Class<?> repositoryInterface, Class<?> entityClass, Class<?> idClass, String csvPath) {
            this.repositoryInterface = repositoryInterface;
            this.entityClass = entityClass;
            this.idClass = idClass;
            this.csvPath = csvPath;
        }

        @Override
        public Class<?> getIdType() {
            return idClass;
        }

        @Override
        public Class<?> getDomainType() {
            return entityClass;
        }

        @Override
        public Class<?> getRepositoryInterface() {
            return repositoryInterface;
        }

        @Override
        public org.springframework.data.util.TypeInformation<?> getIdTypeInformation() {
            return org.springframework.data.util.TypeInformation.of(idClass);
        }

        @Override
        public org.springframework.data.util.TypeInformation<?> getDomainTypeInformation() {
            return org.springframework.data.util.TypeInformation.of(entityClass);
        }

        @Override
        public boolean isPagingRepository() {
            return false;
        }

        @Override
        public Class<?> getReturnedDomainClass(java.lang.reflect.Method method) {
            return entityClass;
        }

        @Override
        public boolean isReactiveRepository() {
            return false;
        }

        @Override
        public Set<Class<?>> getAlternativeDomainTypes() {
            return Set.of();
        }

        @Override
        public TypeInformation<?> getReturnType(Method method) {
            return org.springframework.data.util.TypeInformation.of(method.getReturnType());
        }

        public Set<RepositoryFragment<?>> getFragments() {
            return Set.of();
        }

        public CrudMethods getCrudMethods() {
            org.springframework.data.util.TypeInformation<?> repositoryType =
                    org.springframework.data.util.TypeInformation.of(repositoryInterface);
            org.springframework.data.util.TypeInformation<?> domainType =
                    org.springframework.data.util.TypeInformation.of(entityClass);

            try {
                // Try to use a factory method if available
                java.lang.reflect.Method ofMethod =
                        org.springframework.data.repository.core.CrudMethods.class
                                .getDeclaredMethod("of",
                                        org.springframework.data.util.TypeInformation.class,
                                        org.springframework.data.util.TypeInformation.class);
                return (CrudMethods) ofMethod.invoke(null, repositoryType, domainType);
            } catch (NoSuchMethodException e) {
                // If factory method doesn't exist, try to find the actual implementation class
                try {
                    // Look for DefaultCrudMethods or similar implementation in the same package
                    String[] possibleClassNames = {
                        "DefaultCrudMethods",
                        "CrudMethods$DefaultCrudMethods",
                        "org.springframework.data.repository.core.DefaultCrudMethods"
                    };

                    for (String className : possibleClassNames) {
                        try {
                            Class<?> implClass = Class.forName(className);
                            if (CrudMethods.class.isAssignableFrom(implClass) &&
                                !java.lang.reflect.Modifier.isAbstract(implClass.getModifiers())) {
                                java.lang.reflect.Constructor<?> constructor = implClass.getDeclaredConstructor(
                                        org.springframework.data.util.TypeInformation.class,
                                        org.springframework.data.util.TypeInformation.class);
                                constructor.setAccessible(true);
                                return (CrudMethods) constructor.newInstance(repositoryType, domainType);
                            }
                        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                            // Try next class name
                        }
                    }

                    // Look in declared classes
                    Class<?>[] classes = org.springframework.data.repository.core.CrudMethods.class.getDeclaredClasses();
                    for (Class<?> clazz : classes) {
                        if (CrudMethods.class.isAssignableFrom(clazz) &&
                            !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                            try {
                                java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(
                                        org.springframework.data.util.TypeInformation.class,
                                        org.springframework.data.util.TypeInformation.class);
                                constructor.setAccessible(true);
                                return (CrudMethods) constructor.newInstance(repositoryType, domainType);
                            } catch (NoSuchMethodException ignored) {
                                // Try next class
                            }
                        }
                    }
                } catch (Exception ex) {
                    // If all else fails, throw exception
                    throw new UnsupportedOperationException("Cannot create CrudMethods instance. " +
                            "This may be due to Spring Data Commons API changes.", ex);
                }
            } catch (Exception e) {
                throw new UnsupportedOperationException("Cannot create CrudMethods instance. " +
                        "This may be due to Spring Data Commons API changes.", e);
            }

            throw new UnsupportedOperationException("Cannot create CrudMethods instance. " +
                    "No suitable implementation found. This may be due to Spring Data Commons API changes.");
        }

        public Class<?> getEntityClass() {
            return entityClass;
        }

        public Class<?> getIdClass() {
            return idClass;
        }

        public String getCsvPath() {
            return csvPath;
        }
    }
}

