package dev.snowdrop.mtool.mcp;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import java.util.List;

import dev.snowdrop.mtool.validate.Validator;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkiverse.mcp.server.ToolInputGuardrail;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Singleton
public class ValidatorNameGuardrail implements ToolInputGuardrail {

    private static final Logger LOG = Logger.getLogger(ValidatorNameGuardrail.class);
    private static final String VALIDATOR_SUFFIX = "Validator";

    @ConfigProperty(name = "mtool.validator-packages", defaultValue = "dev.snowdrop.mtool.validate")
    List<String> validatorPackages;

    private Set<String> validatorNames;

    @PostConstruct
    void init() {
        validatorNames = discoverValidatorNames();
        LOG.infof("Discovered validators: %s", validatorNames);
    }

    @Override
    public void apply(ToolInputContext context) {
        JsonObject args = context.getArguments();
        String validator = args.getString("validator");
        if (validator == null || "all".equalsIgnoreCase(validator)) {
            return;
        }

        if (!validatorNames.contains(validator)) {
            String available = validatorNames.stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new ToolCallException(
                    "Unknown validator: '" + validator + "'. "
                            + "Available validators are: " + available + ", or 'all' to run them all.");
        }
    }

    private Set<String> discoverValidatorNames() {
        Set<String> names = new TreeSet<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        for (String packageName : validatorPackages) {
            String path = packageName.trim().replace('.', '/');
            try {
                Enumeration<URL> resources = cl.getResources(path);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    if ("file".equals(url.getProtocol())) {
                        scanDirectory(new File(url.toURI()), packageName.trim(), cl, names);
                    } else if ("jar".equals(url.getProtocol())) {
                        scanJar(url, path, cl, names);
                    }
                }
            } catch (IOException | URISyntaxException e) {
                LOG.warnf("Could not scan package '%s' for validators: %s", packageName, e.getMessage());
            }
        }

        return Collections.unmodifiableSet(names);
    }

    private void scanDirectory(File dir, String packageName, ClassLoader cl, Set<String> names) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), cl, names);
            } else if (isValidatorClassFile(file.getName())) {
                String className = packageName + "." + file.getName().replace(".class", "");
                addIfValidator(className, cl, names);
            }
        }
    }

    private void scanJar(URL url, String basePath, ClassLoader cl, Set<String> names) throws IOException {
        JarURLConnection jarConn = (JarURLConnection) url.openConnection();
        try (JarFile jar = jarConn.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.startsWith(basePath + "/")
                        && entryName.endsWith(VALIDATOR_SUFFIX + ".class")
                        && !entryName.endsWith("/Validator.class")) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    addIfValidator(className, cl, names);
                }
            }
        }
    }

    private boolean isValidatorClassFile(String fileName) {
        return fileName.endsWith(VALIDATOR_SUFFIX + ".class")
                && !fileName.equals("Validator.class");
    }

    private void addIfValidator(String className, ClassLoader cl, Set<String> names) {
        try {
            Class<?> clazz = cl.loadClass(className);
            if (Validator.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                String kebab = toKebabCase(clazz.getSimpleName().replace(VALIDATOR_SUFFIX, ""));
                names.add(kebab);
            }
        } catch (ClassNotFoundException e) {
            LOG.debugf("Could not load class %s", className);
        }
    }

    private static String toKebabCase(String pascalCase) {
        return pascalCase.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}