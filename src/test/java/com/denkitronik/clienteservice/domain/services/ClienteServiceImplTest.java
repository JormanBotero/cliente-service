package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests — ClienteServiceImpl con Mockito")
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao;        // mock: no toca la BD

    @InjectMocks
    private ClienteServiceImpl clienteService;  // instancia real con el mock inyectado

    private Cliente cliente;
    private Region region;

    @BeforeEach
    void setUp() {
        // Arrange compartido: datos de prueba reutilizados en todos los tests
        region = new Region();
        region.setId(1L);
        region.setNombre("Sudamérica");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Linus");
        cliente.setApellido("Torvalds");
        cliente.setEmail("linus@kernel.org");
        cliente.setRegion(region);
    }
    @Test
    @DisplayName("findById — ID existente → devuelve el cliente")
    void findById_idExistente_debeRetornarCliente() {
        // Arrange: programamos el mock
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));

        // Act: llamamos al método real del servicio
        Cliente resultado = clienteService.findById(1L);

        // Assert: verificamos el resultado
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus");
        // Verificamos que el DAO fue consultado exactamente una vez
        verify(clienteDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById — ID inexistente → lanza ClienteNotFoundException")
    void findById_idInexistente_debeLanzarExcepcion() {
        // Arrange: el mock devuelve vacío — el ID no existe
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert: verificamos que se lanza la excepción correcta
        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }
    @Test
    @DisplayName("save — email duplicado → lanza ClienteServiceException")
    void save_emailDuplicado_debeLanzarClienteServiceException() {
        // Arrange: simulamos que la BD rechaza por email duplicado
        when(clienteDao.save(cliente))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        // Act + Assert
        assertThatThrownBy(() -> clienteService.save(cliente))
                .isInstanceOf(ClienteServiceException.class)
                .hasMessageContaining("duplicados");
    }
    @Test
    @DisplayName("delete — ID inexistente → lanza excepción sin llamar deleteById")
    void delete_idInexistente_noDebeEliminar() {
        when(clienteDao.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.delete(999L))
                .isInstanceOf(ClienteNotFoundException.class);

        // Verificación de comportamiento: deleteById NUNCA debe llamarse
        verify(clienteDao, never()).deleteById(any());
    }
    @Test
    @DisplayName("update — ID existente → actualiza y retorna cliente modificado")
    void update_idExistente_debeActualizarYRetornar() {
        // Arrange
        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Linus Updated");
        datosNuevos.setApellido("Torvalds");
        datosNuevos.setEmail("linus2@kernel.org");
        datosNuevos.setRegion(region);

        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteDao.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Cliente resultado = clienteService.update(1L, datosNuevos);

        // Assert
        assertThat(resultado.getNombre()).isEqualTo("Linus Updated");
        assertThat(resultado.getEmail()).isEqualTo("linus2@kernel.org");
        verify(clienteDao, times(1)).findById(1L);
        verify(clienteDao, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("update — ID inexistente → lanza ClienteNotFoundException")
    void update_idInexistente_debeLanzarExcepcion() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.update(999L, cliente))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── Tests agregados para subir la cobertura ───────────────────────────────

    @Test
    @DisplayName("findAll() — devuelve todos los clientes")
    void findAll_debeRetornarTodosLosClientes() {
        when(clienteDao.findAll()).thenReturn(List.of(cliente));

        List<Cliente> resultado = clienteService.findAll();

        assertThat(resultado).hasSize(1).containsExactly(cliente);
        verify(clienteDao).findAll();
    }

    @Test
    @DisplayName("findAll(Pageable) — devuelve la página solicitada")
    void findAllPaginado_debeRetornarLaPagina() {
        Pageable pageable = PageRequest.of(0, 4);
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente), pageable, 1);
        when(clienteDao.findAll(pageable)).thenReturn(pagina);

        Page<Cliente> resultado = clienteService.findAll(pageable);

        assertThat(resultado.getContent()).containsExactly(cliente);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("save — datos válidos → devuelve el cliente guardado")
    void save_datosValidos_debeRetornarClienteGuardado() {
        when(clienteDao.save(cliente)).thenReturn(cliente);

        Cliente resultado = clienteService.save(cliente);

        assertThat(resultado).isSameAs(cliente);
        verify(clienteDao).save(cliente);
    }

    @Test
    @DisplayName("delete — ID existente → elimina el cliente")
    void delete_idExistente_debeEliminar() {
        when(clienteDao.existsById(1L)).thenReturn(true);

        clienteService.delete(1L);

        verify(clienteDao).deleteById(1L);
    }

    @Test
    @DisplayName("findAllRegiones — devuelve las regiones del DAO")
    void findAllRegiones_debeRetornarRegiones() {
        when(clienteDao.findAllRegiones()).thenReturn(List.of(region));

        List<Region> resultado = clienteService.findAllRegiones();

        assertThat(resultado).containsExactly(region);
    }

    @Test
    @DisplayName("ClienteServiceException(String) — conserva el mensaje")
    void clienteServiceException_soloMensaje_debeConservarMensaje() {
        ClienteServiceException ex = new ClienteServiceException("algo falló");

        assertThat(ex).hasMessage("algo falló").hasNoCause();
    }

    @Test
    @DisplayName("ClienteNotFoundException — expone el id y el mensaje")
    void clienteNotFoundException_debeExponerId() {
        ClienteNotFoundException ex = new ClienteNotFoundException(42L);

        assertThat(ex.getId()).isEqualTo(42L);
        assertThat(ex).hasMessageContaining("42");
    }
}