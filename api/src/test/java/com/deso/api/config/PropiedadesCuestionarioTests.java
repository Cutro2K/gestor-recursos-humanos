package com.deso.api.config;

import com.deso.api.model.entity.Candidato;
import com.deso.api.model.entity.Cuestionario;
import com.deso.api.model.entity.Evaluacion;
import com.deso.api.model.enums.EstadoCuestionario;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

class PropiedadesCuestionarioTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void cargaValoresPorDefectoDesdeApplicationPropertiesYAplicaEnCuestionario() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            PropiedadesCuestionario propiedades = context.getBean(PropiedadesCuestionario.class);

            assertThat(propiedades.instrucciones()).isNotBlank();
            assertThat(propiedades.cantidadMaximaAccesos()).isEqualTo(3);
            assertThat(propiedades.minutosMaximosActivo()).isEqualTo(1440);
            assertThat(propiedades.minutosMaximosResolucion()).isEqualTo(60);
            assertThat(propiedades.preguntasPorBloque()).isEqualTo(2);

            Cuestionario cuestionario = new Cuestionario(new Evaluacion(), new Candidato(), "A1B2C3D4", propiedades);
            assertThat(cuestionario.getEstado()).isEqualTo(EstadoCuestionario.ACTIVO);
            assertThat(cuestionario.getCantidadAccesos()).isZero();
            assertThat(cuestionario.getCantidadMaximaAccesos()).isEqualTo(3);
            assertThat(cuestionario.getMinutosMaximosActivo()).isEqualTo(1440);
            assertThat(cuestionario.getMinutosMaximosResolucion()).isEqualTo(60);
            assertThat(cuestionario.getPreguntasPorBloque()).isEqualTo(2);
        });
    }

    @Test
    void fallaAlIniciarSiAlgunParametroEsInvalido() {
        contextRunner
                .withPropertyValues("cuestionario.preguntas-por-bloque=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PropiedadesCuestionario.class)
    static class TestConfig {
        @Bean
        static LocalValidatorFactoryBean configurationPropertiesValidator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
