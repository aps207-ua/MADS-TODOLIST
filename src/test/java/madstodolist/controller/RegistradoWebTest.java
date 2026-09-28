package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
}