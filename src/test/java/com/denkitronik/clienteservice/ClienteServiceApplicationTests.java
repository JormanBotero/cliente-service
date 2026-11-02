package com.denkitronik.clienteservice;

import com.denkitronik.clienteservice.delivery.exception.GlobalExceptionHandler;
import com.denkitronik.clienteservice.delivery.rest.ClienteRestController;
import com.denkitronik.clienteservice.domain.services.IClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contexttest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Smoke tests — contexto completo de Spring con H2")
class ClienteServiceApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("contextLoads — el contexto arranca y registra los beans principales")
    void contextLoads() {
        assertThat(context.getBean(ClienteRestController.class)).isNotNull();
        assertThat(context.getBean(IClienteService.class)).isNotNull();
        assertThat(context.getBean(GlobalExceptionHandler.class)).isNotNull();
    }

    @Test
    @DisplayName("main — arranca la aplicación sin servidor web y con H2 (cubre el main)")
    void main_debeArrancarLaAplicacion() {
        assertThatCode(() -> ClienteServiceApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.datasource.url=jdbc:h2:mem:maintest;DB_CLOSE_DELAY=-1",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jpa.hibernate.ddl-auto=create-drop"
        })).doesNotThrowAnyException();
    }
}