package com.vgrfinance.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RN21: "hoje" é a data de Brasília, mesmo com o servidor em UTC. */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
