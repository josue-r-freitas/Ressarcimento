package br.com.empresa.ressarcimento.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RessarcimentoProperties.class)
public class RessarcimentoConfiguration {
}
