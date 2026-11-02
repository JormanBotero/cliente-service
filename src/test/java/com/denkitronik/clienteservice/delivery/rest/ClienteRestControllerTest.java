package com.denkitronik.clienteservice.delivery.rest;

import com.denkitronik.clienteservice.delivery.exception.GlobalExceptionHandler;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.services.IClienteService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteRestController.class)
@DisplayName("Integration tests (web slice) — ClienteRestController con MockMvc")
class ClienteRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean                         // reemplaza IClienteService en el contexto de Spring
    private IClienteService clienteService;

    @Autowired
    private ObjectMapper objectMapper;  // para serializar objetos a JSON

    private Cliente cliente;
    private static final String BASE = "/api/v1/cliente-service";

    @BeforeEach
    void setUp() {
        Region region = new Region();
        region.setId(4L);
        region.setNombre("Europa");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ada");
        cliente.setApellido("Lovelace");
        cliente.setEmail("ada@babbage.uk");
        cliente.setRegion(region);
    }
    @Test
    @DisplayName("GET /clientes/1 → 200 con el cliente correcto")
    void buscarCliente_idExistente_debeRetornar200ConCliente() throws Exception {
        when(clienteService.findById(1L)).thenReturn(cliente);

        mockMvc.perform(get(BASE + "/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ada"))
                .andExpect(jsonPath("$.apellido").value("Lovelace"));
    }

    @Test
    @DisplayName("GET /clientes/999 → 404 cuando el cliente no existe")
    void buscarCliente_idInexistente_debeRetornar404() throws Exception {
        when(clienteService.findById(999L))
                .thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(get(BASE + "/clientes/999"))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("POST /clientes con nombre vacío → 400 con lista de errores")
    void crearCliente_nombreVacio_debeRetornar400() throws Exception {
        cliente.setNombre("");  // viola @NotEmpty @Size(min=2)

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("nombre"))));
    }

    @Test
    @DisplayName("POST /clientes con email inválido → 400")
    void crearCliente_emailInvalido_debeRetornar400() throws Exception {
        cliente.setEmail("esto-no-es-un-email");

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("PUT /clientes/1 válido → 201 Created")
    void actualizarCliente_valido_debeRetornar201() throws Exception {
        when(clienteService.update(eq(1L), any(Cliente.class))).thenReturn(cliente);

        mockMvc.perform(put(BASE + "/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /clientes/999 → 404 cuando el cliente no existe")
    void actualizarCliente_idInexistente_debeRetornar404() throws Exception {
        when(clienteService.update(eq(999L), any(Cliente.class)))
                .thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(put(BASE + "/clientes/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isNotFound());
    }

    // ── Tests agregados para subir la cobertura ───────────────────────────────

    @Test
    @DisplayName("GET /clientes → 200 con la lista de clientes")
    void listarClientes_debeRetornar200ConLista() throws Exception {
        when(clienteService.findAll()).thenReturn(List.of(cliente));

        mockMvc.perform(get(BASE + "/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Ada"));
    }

    @Test
    @DisplayName("GET /clientes/page/0 → 200 con la página de clientes")
    void listarClientesPaginado_debeRetornar200ConPagina() throws Exception {
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente), PageRequest.of(0, 4), 1);
        when(clienteService.findAll(PageRequest.of(0, 4))).thenReturn(pagina);

        mockMvc.perform(get(BASE + "/clientes/page/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Ada"));
    }

    @Test
    @DisplayName("POST /clientes válido → 201 Created con el cliente guardado")
    void crearCliente_valido_debeRetornar201() throws Exception {
        when(clienteService.save(any(Cliente.class))).thenReturn(cliente);

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /clientes con error del servicio → 500 con ApiErrorResponse")
    void crearCliente_errorDeServicio_debeRetornar500() throws Exception {
        when(clienteService.save(any(Cliente.class)))
                .thenThrow(new ClienteServiceException("fallo", new RuntimeException("db")));

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.mensaje", containsString("Error interno")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET con excepción inesperada → 500 genérico")
    void excepcionInesperada_debeRetornar500Generico() throws Exception {
        when(clienteService.findById(7L)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get(BASE + "/clientes/7"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensaje", containsString("Error inesperado")));
    }

    @Test
    @DisplayName("PUT /clientes/1 con email inválido → 400 y no llama al servicio")
    void actualizarCliente_emailInvalido_debeRetornar400() throws Exception {
        cliente.setEmail("esto-no-es-un-email");

        mockMvc.perform(put(BASE + "/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("DELETE /clientes/1 → 204 No Content")
    void eliminarCliente_idExistente_debeRetornar204() throws Exception {
        doNothing().when(clienteService).delete(1L);

        mockMvc.perform(delete(BASE + "/clientes/1"))
                .andExpect(status().isNoContent());

        verify(clienteService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /clientes/999 → 404 cuando el cliente no existe")
    void eliminarCliente_idInexistente_debeRetornar404() throws Exception {
        doThrow(new ClienteNotFoundException(999L)).when(clienteService).delete(999L);

        mockMvc.perform(delete(BASE + "/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje", containsString("999")));
    }

    @Test
    @DisplayName("GET /clientes/regiones → 200 con la lista de regiones")
    void listarRegiones_debeRetornar200ConRegiones() throws Exception {
        when(clienteService.findAllRegiones()).thenReturn(List.of(cliente.getRegion()));

        mockMvc.perform(get(BASE + "/clientes/regiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Europa"));
    }

    @Test
    @DisplayName("GlobalExceptionHandler.manejarValidacion → 400 con mapa campo → mensaje")
    void manejarValidacion_debeRetornar400ConMapaDeErrores() throws Exception {
        // Con BindingResult en el controlador esta excepción nunca llega al handler por HTTP,
        // así que se prueba el handler directamente.
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(cliente, "cliente");
        binding.addError(new FieldError("cliente", "nombre", "El nombre no puede estar vacio"));
        binding.addError(new FieldError("cliente", "email", "Debe ser un email valido"));
        MethodParameter parametro = new MethodParameter(Object.class.getMethod("toString"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parametro, binding);

        ResponseEntity<Map<String, String>> respuesta =
                new GlobalExceptionHandler().manejarValidacion(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody())
                .containsEntry("nombre", "El nombre no puede estar vacio")
                .containsEntry("email", "Debe ser un email valido");
    }
}