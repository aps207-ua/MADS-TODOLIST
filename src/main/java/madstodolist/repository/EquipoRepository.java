package madstodolist.repository;

import madstodolist.model.Equipo;
import madstodolist.model.Tarea;
import org.springframework.data.repository.CrudRepository;

public interface EquipoRepository extends CrudRepository<Equipo, Long> {
}
