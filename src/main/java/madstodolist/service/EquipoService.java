package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.yaml.snakeyaml.events.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EquipoService {

    Logger logger = LoggerFactory.getLogger(TareaService.class);

    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public EquipoData crearEquipo(String nombre) {
        logger.debug("creando equipo con nombre "+nombre+".");

        if(nombre.isBlank()){
            throw new EquipoServiceException("El nombre es nulo o caracter en blanco");
        }
        logger.debug("Nombre en formato correcto");

        Optional<Equipo> equipoBD = equipoRepository.findByNombre(nombre);

        if(equipoBD.isPresent()){
            throw new EquipoServiceException("Equipo encontrado con ese nombre");
        }
        logger.debug("Equipo con ese nombre inexistente");

        Equipo equipo = new Equipo(nombre);
        equipo = equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public EquipoData recuperarEquipo(Long id) {
        logger.debug("Recuperando equipo con id "+id+".");

        Equipo equipo= equipoRepository.findById(id).orElse(null);
        if (equipo == null) return null;
        else {
            return modelMapper.map(equipo, EquipoData.class);
        }
    }

    @Transactional(readOnly = true)
    public List<EquipoData> findAllOrdenadoPorNombre(){
        List<Equipo> equiposLista = equipoRepository.findAll();
        if(equiposLista.isEmpty()){
            return new ArrayList<EquipoData>();
        }
        equiposLista = Equipo.ordenarPorNombre(equiposLista);
        List<EquipoData> equipoDataList= new ArrayList<>();
        for(Equipo equipo : equiposLista){
            equipoDataList.add(modelMapper.map(equipo, EquipoData.class));
        }
        return equipoDataList;
    }
}