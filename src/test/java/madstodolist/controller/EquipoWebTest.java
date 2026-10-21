package madstodolist.controller;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private EquipoService equipoService;

    @Test
    public void listaEquiposDevuelveVistaYListaDeEquipos() throws Exception {
        // Mock de usuario
        UsuarioData usuarioMock = new UsuarioData();
        usuarioMock.setId(1L);
        usuarioMock.setNombre("Usuario Test");
        when(usuarioService.findById(1L)).thenReturn(usuarioMock);

        // Mock de equipos
        EquipoData equipo1 = new EquipoData();
        equipo1.setId(1L);
        equipo1.setNombre("Proyecto Alpha");

        EquipoData equipo2 = new EquipoData();
        equipo2.setId(2L);
        equipo2.setNombre("Proyecto Beta");

        List<EquipoData> equiposMock = Arrays.asList(equipo1, equipo2);
        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(equiposMock);

        // Mocks adicionales por si la plantilla consulta miembros de equipos
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());
        when(equipoService.usuariosEquipo(2L)).thenReturn(Collections.emptyList());

        // Sesión completa
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", 1L);
        session.setAttribute("nombreUsuarioLogeado", "Usuario Test");
        session.setAttribute("esAdmin", false);

        this.mockMvc.perform(get("/equipos").session(session))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("formEquipos"))
                .andExpect(model().attributeExists("equipos"))
                .andExpect(model().attribute("equipos", equiposMock))
                .andExpect(content().string(containsString("Proyecto Alpha")))
                .andExpect(content().string(containsString("Proyecto Beta")));
    }

    @Test
    public void listaEquiposVaciaMuestraListaVacia() throws Exception {
        UsuarioData usuarioMock = new UsuarioData();
        usuarioMock.setId(1L);
        when(usuarioService.findById(1L)).thenReturn(usuarioMock);

        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(Collections.emptyList());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", 1L);
        session.setAttribute("nombreUsuarioLogeado", "Usuario Test");

        this.mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("formEquipos"))
                .andExpect(model().attributeExists("equipos"))
                .andExpect(model().attribute("equipos", Collections.emptyList()));
    }

    @Test
    public void listaEquiposSinUsuarioLogeadoDevuelve403() throws Exception {
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isForbidden());
    }
}