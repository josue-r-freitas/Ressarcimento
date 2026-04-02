package br.com.empresa.ressarcimento;

import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.com.empresa.ressarcimento.config.EnvFileLoader;

@SpringBootApplication
public class RessarcimentoApplication {

    public static void main(String[] args) {
        Path cwd = Path.of(System.getProperty("user.dir", ".")).normalize();
        EnvFileLoader.applyJdbcKeysToSystemPropertiesUnlessJvmSet(cwd);
        SpringApplication.run(RessarcimentoApplication.class, args);
    }
}

