package br.com.empresa.ressarcimento.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Regista no {@link org.springframework.core.env.Environment} o conteúdo dos ficheiros de ambiente.
 * O {@link EnvFileLoader#applyJdbcKeysToSystemPropertiesUnlessJvmSet(Path)} em {@code main} garante
 * credenciais JDBC antes do arranque do Spring.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "dotenv";
    private static final String PREFIX = "RESSARCIMENTO_";
    private static final DeferredLog LOG = new DeferredLog();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path cwd = Path.of(System.getProperty("user.dir", ".")).normalize();
        Path r = cwd.resolve("ressarcimento.env");
        Path e = cwd.resolve(".env");
        Map<String, String> raw = EnvFileLoader.loadMergedRaw(cwd);
        Map<String, Object> map = new LinkedHashMap<>();
        List<Path> loaded = new ArrayList<>();
        if (!raw.isEmpty()) {
            if (Files.isRegularFile(r)) {
                loaded.add(r);
            }
            if (Files.isRegularFile(e)) {
                loaded.add(e);
            }
        }
        for (Map.Entry<String, String> en : raw.entrySet()) {
            String k = en.getKey();
            String v = en.getValue();
            if (!k.startsWith(PREFIX) && osDefinesNonBlank(k)) {
                continue;
            }
            map.put(k, v);
        }
        if (map.isEmpty()) {
            System.err.println("[ressarcimento] env: nenhuma variável carregada. Procurei em: "
                    + r.toAbsolutePath() + " e " + e.toAbsolutePath()
                    + " (user.dir=" + cwd.toAbsolutePath() + ").");
            return;
        }
        applySpringDatasourceAliases(map);
        if (environment.getPropertySources().contains(SOURCE_NAME)) {
            environment.getPropertySources().remove(SOURCE_NAME);
        }
        environment.getPropertySources().addAfter(
                StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                new MapPropertySource(SOURCE_NAME, map));
        String names = loaded.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
        LOG.info("dotenv: aplicadas " + map.size() + " entradas a partir de: " + names);
        if (map.containsKey(EnvFileLoader.PASSWORD_KEY)) {
            LOG.info("dotenv: " + EnvFileLoader.PASSWORD_KEY + " presente (comprimento="
                    + map.get(EnvFileLoader.PASSWORD_KEY).toString().length() + ")");
        } else {
            LOG.warn("dotenv: " + EnvFileLoader.PASSWORD_KEY + " ausente");
        }
        printDiagnostics(loaded, map);
    }

    private static void applySpringDatasourceAliases(Map<String, Object> map) {
        if (map.containsKey(EnvFileLoader.PASSWORD_KEY)) {
            map.put("spring.datasource.password", map.get(EnvFileLoader.PASSWORD_KEY));
        }
        if (map.containsKey(EnvFileLoader.USERNAME_KEY)) {
            map.put("spring.datasource.username", map.get(EnvFileLoader.USERNAME_KEY));
        }
        if (map.containsKey(EnvFileLoader.URL_KEY)) {
            map.put("spring.datasource.url", map.get(EnvFileLoader.URL_KEY));
        }
    }

    private static void printDiagnostics(List<Path> loaded, Map<String, Object> map) {
        boolean hasPwd = map.containsKey(EnvFileLoader.PASSWORD_KEY) || map.containsKey("spring.datasource.password");
        int len = 0;
        if (map.containsKey(EnvFileLoader.PASSWORD_KEY)) {
            len = map.get(EnvFileLoader.PASSWORD_KEY).toString().length();
        } else if (map.containsKey("spring.datasource.password")) {
            len = map.get("spring.datasource.password").toString().length();
        }
        String jvmPwd = System.getProperty(EnvFileLoader.PASSWORD_KEY);
        String jvmDsPwd = System.getProperty("spring.datasource.password");
        String paths = loaded.stream().map(p -> p.toAbsolutePath().toString()).collect(Collectors.joining(" + "));
        System.err.println("[ressarcimento] env: " + paths
                + " | entradas=" + map.size()
                + " | senha BD: " + (hasPwd ? "sim (" + len + " caracteres)" : "não")
                + ((jvmPwd != null && !jvmPwd.isEmpty()) || (jvmDsPwd != null && !jvmDsPwd.isEmpty())
                        ? " | JVM -D sobrescreve credenciais" : ""));
    }

    private static boolean osDefinesNonBlank(String key) {
        String v = System.getenv(key);
        return v != null && !v.isBlank();
    }
}
