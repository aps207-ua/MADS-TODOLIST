package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
//
// A diferencia de los tests web de tarea, donde usábamos los datos
// de prueba de la base de datos, aquí vamos a practicar otro enfoque:
// moquear el usuarioService.
public class UsuarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    // Moqueamos el usuarioService.
    // En los tests deberemos proporcionar el valor devuelto por las llamadas
    // a los métodos de usuarioService que se van a ejecutar cuando se realicen
    // las peticiones a los endpoint.
    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void servicioLoginUsuarioOK() throws Exception {
        // GIVEN
        // Moqueamos la llamada a usuarioService.login para que
        // devuelva un LOGIN_OK y la llamada a usuarioServicie.findByEmail
        // para que devuelva un usuario determinado.

        UsuarioData anaGarcia = new UsuarioData();
        anaGarcia.setNombre("Ana García");
        anaGarcia.setId(1L);

        when(usuarioService.login("ana.garcia@gmail.com", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("ana.garcia@gmail.com"))
                .thenReturn(anaGarcia);

        // WHEN, THEN
        // Realizamos una petición POST al login pasando los datos
        // esperados en el mock, la petición devolverá una redirección a la
        // URL con las tareas del usuario

        this.mockMvc.perform(post("/login")
                        .param("eMail", "ana.garcia@gmail.com")
                        .param("password", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/1/tareas"));
    }

    @Test
    public void formularioRegistroMuestraSelectorSiNoHayAdmin() throws Exception {
        when(usuarioService.AreThereAnyAdmins()).thenReturn(false);

        this.mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"rol\"")));
    }

    @Test
    public void formularioRegistroOcultaSelectorSiYaExisteAdmin() throws Exception {
        when(usuarioService.AreThereAnyAdmins()).thenReturn(true);

        this.mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"rol\""))));
    }

    @Test
    public void servicioLoginUsuarioAdminRedirigeALaListaDeUsuarios() throws Exception {
        UsuarioData admin = new UsuarioData();
        admin.setId(99L);
        admin.setNombre("Admin");
        admin.setEmail("admin@ua");
        admin.setAdmin(true);

        when(usuarioService.login("admin@ua", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("admin@ua"))
                .thenReturn(admin);

        this.mockMvc.perform(post("/login")
                        .param("eMail", "admin@ua")
                        .param("password", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));
    }

    @Test
    public void adminPuedeEntrarALaListaDeUsuarios() throws Exception {
        UsuarioData admin = new UsuarioData();
        admin.setId(99L);
        admin.setNombre("Admin");
        admin.setEmail("admin@ua");
        admin.setAdmin(true);

        when(usuarioService.login("admin@ua", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("admin@ua"))
                .thenReturn(admin);
        when(usuarioService.findById(99L))
                .thenReturn(admin);
        when(usuarioService.findAll())
                .thenReturn(java.util.List.of(admin));

        MvcResult result = this.mockMvc.perform(post("/login")
                        .param("eMail", "admin@ua")
                        .param("password", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        this.mockMvc.perform(get("/registrados").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lista de Registrados")));
    }

    @Test
    public void noSePuedeEntrarEnDetallesDeOtroUsuarioSiNoEsAdmin() throws Exception {
        UsuarioData usuarioVisitado = new UsuarioData();
        usuarioVisitado.setId(2L);
        usuarioVisitado.setNombre("Otro usuario");
        usuarioVisitado.setEmail("otro@ua");
        usuarioVisitado.setAdmin(false);

        UsuarioData usuarioLogeado = new UsuarioData();
        usuarioLogeado.setId(1L);
        usuarioLogeado.setNombre("Ana");
        usuarioLogeado.setEmail("ana@ua");
        usuarioLogeado.setAdmin(false);

        when(usuarioService.findById(2L)).thenReturn(usuarioVisitado);
        when(usuarioService.findById(1L)).thenReturn(usuarioLogeado);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", 1L);
        session.setAttribute("nombreUsuarioLogeado", "Ana");
        session.setAttribute("esAdmin", false);

        MvcResult result = this.mockMvc.perform(get("/registrados/2").session(session))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                .isEqualTo("No autorizado");
    }

    @Test
    public void noSePuedeEntrarALaListaSiNoEsAdmin() throws Exception {
        UsuarioData usuarioLogeado = new UsuarioData();
        usuarioLogeado.setId(1L);
        usuarioLogeado.setNombre("Ana");
        usuarioLogeado.setEmail("ana@ua");
        usuarioLogeado.setAdmin(false);

        when(usuarioService.findById(1L)).thenReturn(usuarioLogeado);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", 1L);
        session.setAttribute("nombreUsuarioLogeado", "Ana");
        session.setAttribute("esAdmin", false);

        MvcResult result = this.mockMvc.perform(get("/registrados").session(session))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                .isEqualTo("No autorizado");
    }

    @Test
    public void servicioLoginUsuarioNotFound() throws Exception {
        // GIVEN
        // Moqueamos el método usuarioService.login para que devuelva
        // USER_NOT_FOUND
        when(usuarioService.login("pepito.perez@gmail.com", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.USER_NOT_FOUND);

        // WHEN, THEN
        // Realizamos una petición POST con los datos del usuario mockeado y
        // se debe devolver una página que contenga el mensaja "No existe usuario"
        this.mockMvc.perform(post("/login")
                        .param("eMail","pepito.perez@gmail.com")
                        .param("password","12345678"))
                .andExpect(content().string(containsString("No existe usuario")));
    }

    @Test
    public void servicioLoginUsuarioErrorPassword() throws Exception {
        // GIVEN
        // Moqueamos el método usuarioService.login para que devuelva
        // ERROR_PASSWORD
        when(usuarioService.login("ana.garcia@gmail.com", "000"))
                .thenReturn(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // WHEN, THEN
        // Realizamos una petición POST con los datos del usuario mockeado y
        // se debe devolver una página que contenga el mensaja "Contraseña incorrecta"
        this.mockMvc.perform(post("/login")
                        .param("eMail","ana.garcia@gmail.com")
                        .param("password","000"))
                .andExpect(content().string(containsString("Contraseña incorrecta")));
    }
}
