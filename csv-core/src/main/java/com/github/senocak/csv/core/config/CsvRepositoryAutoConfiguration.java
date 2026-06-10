package com.github.senocak.csv.core.config;

import com.github.senocak.csv.core.annotation.CsvFile;
import com.github.senocak.csv.core.annotation.EnableCsvRepositories;
import com.github.senocak.csv.core.handler.CsvRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Auto configuration class that registers CSV repository factory beans.
 */
@Configuration
public class CsvRepositoryAutoConfiguration implements ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // Get base packages from @EnableCsvRepositories annotation
        String[] basePackages = getBasePackages(importingClassMetadata);
        log.info("Scanning for CSV repositories in packages: {}", Arrays.toString(basePackages));
        boolean foundAny = false;
        final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        for (String basePackage : basePackages) {
            try {
                // Convert package name to resource path
                final String packageSearchPath = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";
                log.debug("Searching for classes in: {}", packageSearchPath);

                Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
                log.debug("Found {} class files in package {}", resources.length, basePackage);

                for (Resource resource : resources) {
                    try {
                        final String className = getClassNameFromResource(resource, basePackage);
                        if (className != null) {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isInterface() && clazz.isAnnotationPresent(CsvFile.class)) {
                                log.info("Found CSV repository interface: {}", className);
                                registerRepositoryBean(registry, clazz);
                                foundAny = true;
                            }
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        log.debug("Could not load class from resource {}: {}", resource, e.getMessage());
                    } catch (Exception e) {
                        log.warn("Error processing resource {}: {}", resource, e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.error("Failed to scan package {}: {}", basePackage, e.getMessage(), e);
            }
        }
        if (!foundAny) {
            log.warn("No CSV repositories found in packages: {}", Arrays.toString(basePackages));
        } else {
            log.info("Successfully registered CSV repositories");
        }
    }

    @Nullable
    private String getClassNameFromResource(final Resource resource, final String basePackage) {
        try {
            final String resourcePath = resource.getURI().toString();
            final String classPath = resourcePath.substring(resourcePath.indexOf(basePackage.replace('.', '/')));
            return classPath.replace('/', '.').replace(".class", "");
        } catch (Exception e) {
            // Try alternative method
            try {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".class")) {
                    String simpleName = filename.substring(0, filename.length() - 6);
                    return basePackage + "." + simpleName;
                }
            } catch (Exception ex) {
                log.debug("Could not extract class name from resource: {}", resource);
            }
        }
        return null;
    }

    /*
    public void registerBeanDefinitions2(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // Get base packages from @EnableCsvRepositories annotation
        String[] basePackages = getBasePackages(importingClassMetadata);
        log.info("Scanning for CSV repositories in packages: {}", java.util.Arrays.toString(basePackages));

        // Scan for interfaces annotated with @CsvFile
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(CsvFile.class, true)); // true = consider meta-annotations

        boolean foundAny = false;
        for (String basePackage : basePackages) {
            try {
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
                log.debug("Package {}: found {} candidate components", basePackage, candidateComponents.size());
                for (BeanDefinition candidate : candidateComponents) {
                    if (candidate instanceof AbstractBeanDefinition beanDefinition) {
                        String beanClassName = beanDefinition.getBeanClassName();
                        if (beanClassName != null) {
                            try {
                                Class<?> repositoryInterface = Class.forName(beanClassName);
                                if (repositoryInterface.isInterface() && repositoryInterface.isAnnotationPresent(CsvFile.class)) {
                                    log.info("Found CSV repository interface: {}", beanClassName);
                                    registerRepositoryBean(registry, repositoryInterface);
                                    foundAny = true;
                                } else {
                                    log.debug("Skipping {} - not an interface or missing @CsvFile annotation", beanClassName);
                                }
                            } catch (ClassNotFoundException e) {
                                log.error("Failed to load repository interface: " + beanClassName, e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Log but continue scanning other packages
                log.error("Failed to scan package " + basePackage + ": " + e.getMessage(), e);
            }
        }
        
        if (!foundAny) {
            log.warn("No CSV repositories found in packages: " + Arrays.toString(basePackages));
        } else {
            log.info("Successfully registered CSV repositories");
        }
    }
*/

    @NonNull
    private String[] getBasePackages(final @NonNull AnnotationMetadata importingClassMetadata) {
        if (importingClassMetadata.hasAnnotation(EnableCsvRepositories.class.getName())) {
            final Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableCsvRepositories.class.getName());
            if (attributes != null) {
                final String[] basePackages = (String[]) attributes.get("basePackages");
                final Class<?>[] basePackageClasses = (Class<?>[]) attributes.get("basePackageClasses");
                if (basePackages != null && basePackages.length > 0) {
                    return basePackages;
                } else if (basePackageClasses != null && basePackageClasses.length > 0) {
                    return Arrays.stream(basePackageClasses)
                            .map(Class::getPackageName)
                            .toArray(String[]::new);
                }
            }
        }

        // Default to the package of the importing class
        final String className = importingClassMetadata.getClassName();
        if (className != null) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                final String basePackage = className.substring(0, lastDot);
                // Also scan parent packages to find repositories
                return new String[]{
                    basePackage,
                    basePackage + ".repository",
                    basePackage + ".repositories"
                };
            }
        }

        // Fallback: scan common package locations
        return new String[]{
            "com.example.csv.example.repository",
            "com.example.csv.example.repositories",
            "com.example.repository",
            "com.example.repositories"
        };
    }

    private void registerRepositoryBean(BeanDefinitionRegistry registry, Class<?> repositoryInterface) {
        final String beanName = getBeanName(repositoryInterface);
        // Check if bean already exists
        if (registry.containsBeanDefinition(beanName)) {
            log.warn("Repository bean {} already registered, skipping.", beanName);
            return;
        }

        // Create repository proxy using instance supplier
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(repositoryInterface);
        builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);

        final AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        beanDefinition.setInstanceSupplier(() -> CsvRepositoryFactory.createProxy(repositoryInterface));
        beanDefinition.setPrimary(false);
        beanDefinition.setLazyInit(false);
        
        registry.registerBeanDefinition(beanName, beanDefinition);
        log.info("Registered CSV repository bean: {} for interface: {}", beanName, repositoryInterface.getName());
    }

    @NonNull
    private String getBeanName(final @NonNull Class<?> repositoryInterface) {
        final String simpleName = repositoryInterface.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}

