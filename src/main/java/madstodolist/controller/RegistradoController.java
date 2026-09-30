package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;

@Controller
public class RegistradoController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @GetMapping("/registrados/{id}")
    public String detallesRegistrado(@PathVariable(value="id") Long id, Model model, HttpSession session) {
        UsuarioData usuario = usuarioService.findById(id);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        Long usuarioLogeado = managerUserSession.usuarioLogeado();
        if (usuarioLogeado == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        UsuarioData usuarioActual = usuarioService.findById(usuarioLogeado);
        boolean esAdmin = usuarioActual != null && usuarioActual.isAdmin();
        if (!esAdmin && !usuarioLogeado.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("EsAdmin", esAdmin);
        return "detallesRegistrado";
    }

    @GetMapping("/registrados")
    public String listaRegistrados(Model model, HttpSession session) {
        Long usuarioLogeado = managerUserSession.usuarioLogeado();
        if (usuarioLogeado == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        UsuarioData usuarioActual = usuarioService.findById(usuarioLogeado);
        if (usuarioActual == null || !usuarioActual.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        model.addAttribute("usuarios", usuarioService.findAll());
        return "listaRegistrados";
    }
}