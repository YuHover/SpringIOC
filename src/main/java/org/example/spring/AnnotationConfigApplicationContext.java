package org.example.spring;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationConfigApplicationContext {
    private final Class<?> configClazz;
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();


    public AnnotationConfigApplicationContext(Class<?> configClazz) {
        this.configClazz = configClazz;
        this.initContext();
    }

    private void initContext() {
        ComponentScan componentScanAnnotation = this.configClazz.getDeclaredAnnotation(ComponentScan.class);
        String[] basePackagesClassPath = componentScanAnnotation.basePackages();
        ClassLoader applicationClassLoader = this.getClass().getClassLoader();
        for (String packageClassPath : basePackagesClassPath) {
            URL packageURL = applicationClassLoader.getResource(packageClassPath.replace(".", "/"));
            if (packageURL == null) continue;
            File packageDirectory = new File(packageURL.getPath());
            this.walkDirectory(packageDirectory, packageClassPath);
        }

        initSingletonObjects();
    }

    private void walkDirectory(File directory, String classPath) {
        File[] children = directory.listFiles(
                (dir, name) -> name.endsWith(".class") || new File(dir, name).isDirectory()
        );

        if (children == null) return;

        for (File file : children) {
            String fileName = file.getName().replace(".class", "");
            String newClassPath = classPath + "." + fileName;

            if (file.isDirectory()) {
                this.walkDirectory(file, newClassPath);
            }
            else {
                try {
                    Class<?> clazz = Class.forName(newClassPath);
                    if (!clazz.isAnnotationPresent(Component.class)) continue;

                    Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                    String beanName = componentAnnotation.value();
                    if (beanName.isEmpty()) {
                        beanName = this.changeCaseFirst(fileName, false);
                    }

                    ScopeType scope = ScopeType.SINGLETON;
                    if (clazz.isAnnotationPresent(Scope.class)) {
                        scope = clazz.getDeclaredAnnotation(Scope.class).value();
                    }

                    if (this.beanDefinitionMap.containsKey(beanName)) {
                        throw new RuntimeException(beanName + "is duplicated.");
                    }

                    this.beanDefinitionMap.put(beanName, new BeanDefinition(clazz, scope));
                } catch (ClassNotFoundException ignored) { }
            }
        }
    }

    private void initSingletonObjects() {
        for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope() == ScopeType.SINGLETON) {
                this.createBean(beanName, beanDefinition);
            }
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        if (earlySingletonObjects.containsKey(beanName)) {
            return earlySingletonObjects.get(beanName);
        }

        if (singletonObjects.containsKey(beanName)) {
            return singletonObjects.get(beanName);
        }

        Class<?> beanClazz = beanDefinition.getClazz();
        Object earlyBean = this.instantiateBean(beanClazz);
        earlySingletonObjects.put(beanName, earlyBean);

        initializeBean(beanClazz, earlyBean);

        Object singletonBean = earlySingletonObjects.remove(beanName);
        if (beanDefinition.getScope() == ScopeType.SINGLETON) {
            singletonObjects.put(beanName, singletonBean);
        }
        return singletonBean;
    }

    private Object instantiateBean(Class<?> beanClazz) {
        try {
            Constructor<?> constructor = beanClazz.getConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(beanClazz + " doesn't have non-args constructor.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(beanClazz + "'s non-args constructor is inaccessible.", e);
        } catch (InstantiationException e) {
            throw new RuntimeException(beanClazz + " is an abstract class.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeBean(Class<?> beanClazz, Object earlyBean) {
        Field[] fields = beanClazz.getDeclaredFields();  // 无法处理继承关系

        for (Field field : fields) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;

            Class<?> fieldClazz = field.getType();
            boolean required = field.getDeclaredAnnotation(Autowired.class).required();

            Map.Entry<String, BeanDefinition> fieldEntry = null;
            for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
                BeanDefinition definition = entry.getValue();
                if (definition.getClazz().isAssignableFrom(fieldClazz)) {
                    if (fieldEntry != null) {
                        throw new RuntimeException(
                                "Autowired can't determine which "
                                + fieldClazz
                                + " bean to inject into "
                                + beanClazz
                                + "."
                        );
                    }
                    fieldEntry = entry;
                }
            }
            if (fieldEntry == null && required) {
                throw new RuntimeException(
                        "Autowired can't find "
                        + fieldClazz
                        + " bean to inject into "
                        + beanClazz + " and this field is required."
                );
            }

            if (fieldEntry != null) {
                Object fieldBean = createBean(fieldEntry.getKey(), fieldEntry.getValue());
                String setter = "set" + this.changeCaseFirst(field.getName(), true);
                try {
                    beanClazz.getDeclaredMethod(setter, fieldClazz).invoke(earlyBean, fieldBean);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            beanClazz + "'s " + setter + "(" + fieldClazz + ") method is inaccessible.", e
                    );
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(
                            beanClazz + " doesn't have " + setter + "(" + fieldClazz + ") method.", e
                    );
                }
            }
        }
    }

    public Object getBean(String beanName) {
        if (!this.beanDefinitionMap.containsKey(beanName)) {
            return null;
        }

        if (this.singletonObjects.containsKey(beanName)) {
            return this.singletonObjects.get(beanName);
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        return this.createBean(beanName, beanDefinition);
    }

    private String changeCaseFirst(String s, boolean upper) {
        String first = s.substring(0, 1);
        String left = s.substring(1);
        if (upper) return first.toUpperCase() + left;
        return first.toLowerCase() + left;
    }
}
