package br.com.empresa.ressarcimento.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lê {@code ressarcimento.env} e {@code .env} na raiz do projeto (sem depender da ordem de
 * {@link org.springframework.core.env.PropertySource} no Spring).
 */
public final class EnvFileLoader {

    public static final String PASSWORD_KEY = "RESSARCIMENTO_DB_PASSWORD";
    public static final String USERNAME_KEY = "RESSARCIMENTO_DB_USERNAME";
    public static final String URL_KEY = "RESSARCIMENTO_DB_URL";

    private EnvFileLoader() {
    }

    /**
     * Mescla variáveis: primeiro {@code ressarcimento.env}, depois {@code .env} (sobrescreve).
     * Não aplica regras de variáveis de ambiente do SO.
     */
    public static Map<String, String> loadMergedRaw(Path cwd) {
        Map<String, String> map = new LinkedHashMap<>();
        Path r = cwd.resolve("ressarcimento.env");
        Path e = cwd.resolve(".env");
        if (Files.isRegularFile(r)) {
            parseFileInto(r, map);
        }
        if (Files.isRegularFile(e)) {
            parseFileInto(e, map);
        }
        return map;
    }

    /**
     * Define {@code System.setProperty} a partir dos ficheiros para JDBC, exceto se a JVM já tiver
     * a mesma chave com valor não vazio ({@code -D...}). Corre antes de {@link org.springframework.boot.SpringApplication#run}.
     */
    public static void applyJdbcKeysToSystemPropertiesUnlessJvmSet(Path cwd) {
        Map<String, String> m = loadMergedRaw(cwd);
        if (m.isEmpty()) {
            return;
        }
        putUnlessJvmSet(PASSWORD_KEY, m.get(PASSWORD_KEY));
        putUnlessJvmSet(USERNAME_KEY, m.get(USERNAME_KEY));
        putUnlessJvmSet(URL_KEY, m.get(URL_KEY));
        if (m.containsKey(PASSWORD_KEY)) {
            putUnlessJvmSet("spring.datasource.password", m.get(PASSWORD_KEY));
        }
        if (m.containsKey(USERNAME_KEY)) {
            putUnlessJvmSet("spring.datasource.username", m.get(USERNAME_KEY));
        }
        if (m.containsKey(URL_KEY)) {
            putUnlessJvmSet("spring.datasource.url", m.get(URL_KEY));
        }
    }

    private static void putUnlessJvmSet(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String existing = System.getProperty(key);
        if (existing != null && !existing.isBlank()) {
            return;
        }
        System.setProperty(key, value);
    }

    static void parseFileInto(Path file, Map<String, String> map) {
        try {
            byte[] raw = Files.readAllBytes(file);
            Charset charset = detectCharset(raw);
            String text = new String(raw, charset);
            for (String line : text.split("\\R")) {
                String t = line.strip();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq < 1) {
                    continue;
                }
                String key = stripBom(t.substring(0, eq).strip());
                String val = t.substring(eq + 1).strip();
                if (key.isEmpty()) {
                    continue;
                }
                if (val.length() >= 2
                        && ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'")))) {
                    val = val.substring(1, val.length() - 1);
                }
                map.put(key, val);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao ler " + file, ex);
        }
    }

    private static Charset detectCharset(byte[] raw) {
        if (raw.length >= 2 && (raw[0] & 0xFF) == 0xFF && (raw[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (raw.length >= 2 && (raw[0] & 0xFF) == 0xFE && (raw[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }

    private static String stripBom(String key) {
        if (!key.isEmpty() && key.charAt(0) == '\uFEFF') {
            return key.substring(1);
        }
        return key;
    }
}
