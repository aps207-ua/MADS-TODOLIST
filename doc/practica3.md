# Práctica 3: Integración continua y gestión de equipos

Este documento recoge el trabajo realizado desde la versión `v.1.1.0` hasta la versión actual `1.2.0-SNAPSHOT`. Se divide en dos partes: la configuración de integración continua y bases de datos, y la nueva funcionalidad de equipos, que permite crear equipos, unirse a ellos, salir de ellos y, en el caso del administrador, modificarlos y eliminarlos.

Cada funcionalidad la he desarrollado en su propia rama y la he integrado en `main` mediante un pull request (del #17 al #36).

## Integración continua con GitHub Actions

He creado dos workflows en `.github/workflows`, y los dos se ejecutan en cada `push`:

- **`developer-tests.yml`**: lanza `./mvnw test` dentro de un contenedor `eclipse-temurin:17-jdk-alpine`. Usa la base de datos H2 en memoria y es el test rápido que comprueba que nada se ha roto. Al principio el workflow fallaba por la imagen de Java, así que lo cambié para que use un contenedor con JDK. También limité los permisos del token a `contents: read`.
- **`integration-tests.yml`**: arranca un servicio `postgres:13` junto al contenedor de Java y lanza los tests con el perfil `postgres` (`./mvnw test -D spring.profiles.active=postgres`). Tiene un *health check* con `pg_isready` para que los tests no empiecen hasta que PostgreSQL esté listo.

Para comprobar que la integración continua detectaba los errores, subí a propósito un commit con un test que fallaba ("prueba fallo test") y después lo arreglé.

## Perfiles de base de datos

He añadido la dependencia del driver de PostgreSQL en el `pom.xml` y dos ficheros de configuración:

- `src/main/resources/application-postgres.properties`: conexión a la base de datos `mads` en `localhost:5432`.
- `src/test/resources/application-postgres.properties`: conexión a `mads_test`. El host, el puerto, el usuario y la contraseña se leen de variables (`POSTGRES_HOST`, `POSTGRES_PORT`, `DB_USER`, `DB_PASSWD`). Así el mismo fichero funciona en local y en GitHub Actions, donde el host es `postgres`.

Por defecto la aplicación sigue usando el perfil `dev` con H2.

## Modelo: entidad `Equipo`

La entidad `Equipo` la he desarrollado con TDD. Primero escribí los tests en `EquipoTest` (crear un equipo, guardarlo y buscarlo en la base de datos, comprobar la igualdad, la relación con los usuarios...) y después el código para que pasaran.

- **Atributos:** `id` autogenerado y `nombre` obligatorio (`@NotNull`). Se guarda en la tabla `equipos`.
- **Relación muchos a muchos con `Usuario`:** el lado propietario es `Equipo`, con la tabla intermedia `equipo_usuario` (columnas `fk_equipo` y `fk_usuario`). En `Usuario` está el lado inverso, con `@ManyToMany(mappedBy = "usuarios")`.
- **`addUsuario` y `removeUsuario`:** actualizan las dos colecciones a la vez, porque JPA no mantiene sola la relación inversa en memoria.
- **`equals` y `hashCode`:** comparan por `id` si los dos equipos lo tienen y, si no, por `nombre`.
- **`ordenarPorNombre`:** ordena una lista de equipos alfabéticamente con un algoritmo *heap sort* (método `heapify`).

En `EquipoRepository` he añadido `findAll()`, que devuelve una `List`, y `findByNombre()`, que se usa para impedir nombres repetidos. También he actualizado `clean-db.sql` para borrar las tablas `equipo_usuario` y `equipos` antes de cada test.

## Servicio: `EquipoService`

Los servicios también los he hecho con TDD, en `EquipoServiceTest`. Los métodos que modifican datos usan `@Transactional` y los de consulta, `@Transactional(readOnly = true)`. Los errores se comunican con una excepción propia, `EquipoServiceException`.

| Método | Qué hace |
|---|---|
| `crearEquipo(nombre)` | Crea un equipo. Falla si el nombre está en blanco o ya existe. |
| `recuperarEquipo(id)` | Devuelve el equipo o lanza una excepción si no existe. |
| `findAllOrdenadoPorNombre()` | Lista todos los equipos ordenados e incluye en cada `EquipoData` su lista de usuarios. |
| `añadirUsuarioAEquipo(equipo, usuario)` | Añade un usuario a un equipo. Comprueba que existan los dos. |
| `eliminarUsuarioDelEquipo(equipo, usuario)` | Saca a un usuario del equipo. Falla si no pertenece a él. |
| `equiposUsuario(usuario)` | Equipos de un usuario, ordenados por nombre. |
| `usuariosEquipo(equipo)` | Usuarios de un equipo. |
| `actualizarEquipo(equipo, nombre)` | Cambia el nombre. Comprueba que no esté en blanco y que no lo tenga otro equipo. |
| `eliminarEquipo(equipo)` | Borra el equipo. Lanza una excepción si no existe. |

En `actualizarEquipo` no hace falta llamar a `save()`. La entidad que devuelve `findById` dentro de la transacción está gestionada por JPA, y Hibernate guarda los cambios al hacer *commit* (lo que se llama *dirty checking*).

Al hacer el método de eliminar un usuario de un equipo tuve un error porque me había olvidado de poner `@Transactional`. Sin él los cambios en la relación no se guardaban.

Para que la vista pueda mostrar los miembros, he añadido a `EquipoData` una lista de `UsuarioData`.

## Controlador y vistas

### `EquiposController`

Todos los endpoints comprueban primero que haya un usuario en sesión (`idUsuarioLogeado`). Si no lo hay, devuelven `403 Forbidden`. Los de administración comprueban además que el usuario sea administrador.

| Ruta | Quién | Acción |
|---|---|---|
| `GET /equipos` | Usuario logeado | Muestra la lista de equipos (`formEquipos.html`). |
| `POST /equipos/crear` | Usuario logeado | Crea un equipo. |
| `POST /equipos/{id}/unirse` | Usuario logeado | Une al usuario al equipo. |
| `POST /equipos/{id}/salir` | Usuario logeado | Saca al usuario del equipo. |
| `POST /equipos/{id}/update` | Administrador | Cambia el nombre del equipo. |
| `POST /equipos/{id}/delete` | Administrador | Elimina el equipo. |

Todas las acciones redirigen a `/equipos` cuando terminan.

El endpoint de eliminar lo hice primero con `@DeleteMapping`, pero lo cambié a `@PostMapping`. Un formulario HTML solo puede enviar `GET` o `POST`, así que con `DELETE` el botón de la vista nunca habría llegado al controlador. Con `POST` queda igual que el resto de acciones.

### Vistas

- **Barra de navegación (`fragments.html`):** he añadido el enlace "Lista de equipos", visible para cualquier usuario logeado.
- **`formEquipos.html`:** arriba hay un formulario para crear un equipo y debajo, una tabla con todos los equipos. Cada fila tiene estos botones:
  - **Ver Usuarios:** despliega con un `collapse` de Bootstrap la lista de miembros del equipo, o el mensaje "Sin usuarios registrados en este equipo".
  - **Unirse al Equipo / Salir del Equipo:** se muestra uno u otro según si el usuario ya es miembro. Para saberlo se comprueba si su id está en `equipo.usuarios`.
  - **Modificar / Eliminar:** solo aparecen si `session.esAdmin` es verdadero, y están separados de las acciones del usuario. "Modificar" despliega un formulario con el nombre actual ya escrito. "Eliminar" pide confirmación antes de enviar el formulario.
- **`listaRegistrados.html`:** en la lista de usuarios que ve el administrador he añadido una columna "Equipos" con un botón que despliega los equipos de cada usuario.

### Datos de ejemplo

En `InitDbService` (perfil `dev`) he añadido dos equipos de ejemplo: "Proyecto Alpha", con el usuario `user@ua`, y "Proyecto Beta", vacío.

## Tests

En total el proyecto tiene 107 tests y todos pasan. Los nuevos de esta parte son:

- **`EquipoTest` (5):** la entidad y el repositorio.
- **`EquipoServiceTest` (21):** cada método del servicio, tanto los casos correctos como las excepciones. Por ejemplo: nombre en blanco o repetido al crear o actualizar, equipo o usuario inexistente, eliminar a un usuario que no pertenece al equipo, que al eliminar un equipo no se borren sus usuarios...
- **`EquipoWebTest` (24):** el controlador con `MockMvc`, simulando `EquipoService` y `UsuarioService` con `@MockBean`. Comprueba que se devuelva 403 sin sesión o sin ser administrador, las redirecciones y que el servicio se llame con los parámetros correctos. También comprueba el HTML generado: qué botones aparecen según si el usuario es miembro o administrador, y que se muestren los miembros de cada equipo.

También he tenido que actualizar algunos tests antiguos (`RegistradoWebTest` y `UsuarioWebTest`), porque las vistas ahora usan `EquipoService`.

## Uso de la inteligencia artificial
Solo he usado la inteligencia artificial para corregir errores en el codigo y de compilacion.

Todos los cambios propuestos los he revisado y probado antes de integrarlos.
