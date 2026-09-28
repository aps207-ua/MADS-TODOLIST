package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RegistradoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void detallesRegistradoDevuelveVistaYDatosDelUsuario() throws Exception {
        UsuarioData usuarioMock = new UsuarioData();
        usuarioMock.setId(2L);
        usuarioMock.setNombre("Juan Pérez");
        usuarioMock.setEmail("juan@ua.es");

        when(usuarioService.findById(2L)).thenReturn(usuarioMock);

        this.mockMvc.perform(get("/registrados/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("detallesRegistrado"))
                .andExpect(model().attributeExists("usuario"))
                .andExpect(model().attribute("usuario", usuarioMock))
                .andExpect(content().string(containsString("Juan Pérez")))
                .andExpect(content().string(containsString("juan@ua.es")));
    }

    @Test
    public void detallesRegistradoUsuarioNoExistente() throws Exception {
        when(usuarioService.findById(999L)).thenReturn(null);
        this.mockMvc.perform(get("/registrados/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void listaRegistradosVaciaMuestraMensaje() throws Exception {
        // GIVEN
        // El servicio devuelve una lista vacía
        when(usuarioService.findAll()).thenReturn(Collections.emptyList());

        // WHEN & THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(view().name("listaRegistrados"))
                .andExpect(model().attributeExists("usuarios"))
                .andExpect(model().attribute("usuarios", Collections.emptyList()))
                .andExpect(content().string(containsString("No hay usuarios registrados.")));
    }

    @Test
    public void listaRegistradosDevuelveVistaYListaDeUsuarios() throws Exception {
        // GIVEN
        UsuarioData usuario1 = new UsuarioData();
        usuario1.setId(1L);
        usuario1.setNombre("Juan Pérez");
        usuario1.setEmail("juan@ua.es");

        UsuarioData usuario2 = new UsuarioData();
        usuario2.setId(2L);
        usuario2.setNombre("Ana López");
        usuario2.setEmail("ana@ua.es");

        List<UsuarioData> usuariosMock = Arrays.asList(usuario1, usuario2);

        when(usuarioService.findAll()).thenReturn(usuariosMock);

        // WHEN & THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(view().name("listaRegistrados"))
                .andExpect(model().attributeExists("usuarios"))
                .andExpect(model().attribute("usuarios", usuariosMock))
                .andExpect(content().string(containsString("Juan Pérez")))
                .andExpect(content().string(containsString("Ana López")));
    }
}