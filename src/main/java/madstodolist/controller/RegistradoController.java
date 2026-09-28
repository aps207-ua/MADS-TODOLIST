package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class RegistradoController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/registrados/{id}")
    public String detallesRegistrado(@PathVariable(value="id") Long id, Model model) {
        UsuarioData usuario = usuarioService.findById(id);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        model.addAttribute("usuario", usuario);
        return "detallesRegistrado";
    }
}