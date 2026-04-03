![](unrc_logo.png)

**Departamento de Computación**  
**Asignatura: Ingeniería de Software II (Cód. 3387)**  
**Año 2026**

# Guia de Trabajo Practico

## 1. (Requirements) Describir su proyecto:

Sistema de Gestión Académica es una aplicación web desarrollada en Java con Spark Framework que permite administrar usuarios, materias y calificaciones en un entorno universitario. El sistema cuenta con tres roles diferenciados: ***administrador***, ***profesor*** y ***alumno***, cada uno con su propio panel de control y funcionalidades específicas.

### a) Problemas propuestos por nuestro equipo a resolver.

- Poner un limite de intentos a 3 en el login de Usuario.
- Sistema de quejas de alumnos.
- Nuevos roles: *alumno*, *profesor*, *admin*.
- Nueva entidad: **Materia** (nombre, profesor a cargo, calificacion, estudiantes inscriptos, cantidad estudiantes).
- Filtros en la creacion de cuentas.
- Cada rol vea un dashboard distinto.           
- Tiempo de sesión con expiración automática.

--- 

### b) Usuarios y  Funcionalidades.

Tendriamos 3 tipos de usuarios:

- ***Alumnos*** : Las funcionalidades que van a tener son las de inscribirse a las materias, ver sus notas y sus materias disponibles, ver su promedio general calculado automáticamente, ver el historial completo de materias aprobadas, desaprobadas, en curso, ver su legajo o información personal, mandar una consulta al profesor.

- ***Profesores*** : Sus funcionalidades serian ver las materias que tiene asignada, ver el listado de estudiantes inscriptos en cada una (si tiene mas de una materia asignada), cargar notas y modificarlas si se equivoca, ver estadísticas de su materia (promedio de notas, cantidad de aprobados/desaprobados/en curso), recibir consultas de los alumnos.

- ***Administrador*** : El administrador puede crear y gestionar cuentas de profesores y alumnos, administrar las materias del sistema, asignar docentes a las mismas y gestionar el acceso de los usuarios, incluyendo el desbloqueo de cuentas y el reseteo de contraseñas.

***Aclaracion: Se va a usar que el admin resete contraseñas porque es un proyecto universitario, a que utilizar un sistema de envio de emails como lo es normalmente.***

---

### c) Restricciones Tecnicas y Tecnologias Elegidas.

- El proyecto esta desarrollado en **Java** con **Spark Framework**.
- Se utiliza como base de datos local **SQLite con ActiveJDBC** lo que limita que el proyecto escale pero es mas simple.
- Las vistas estan hechas en **Mustache**, un motor de plantillas simple.
- La seguridad de contraseñas se maneja con **BCrypt** y las sesiones son las propias de **Spark**.
- La app va a correr localmente en el **puerto 8080**, sin despliegue en un servidor externo.
- El codigo del proyecto se va a versionar y almacenar en **GitHub** y la gestion del proyecto con los intergrantes del grupo se lleva a cabo en **GitHub Proyects**.

---

### d) Tamaño del equipo y Plazo estimado

 El tamaño de nuestro equipo va a ser de 5 personas y el plazo que creemos que nos va a llevar van a ser 2 meses.

### e) Problemas encontrados

Habiamos encontrado un problema en la recuperacion de contraseña, queriamos hacerla como suele hacerse normalmente que nos llegue un mail para restablecer la contraseña pero al ver que era muy complicado, decidimos hacer que el admin desbloquee a un usuario atraves de un boton para fines practico.

### f) Forma de organización del equipo

Bueno la organizacion de nuestro equipo fue la siguiente con el fin de distribuir las tareas y avanzar lo mas rapidos y seguros posible:

- ***Encargados de la Documentacion y redaccion del mismo***: Delfino Juan y Cibils Mateo.

- ***Codificacion con IA***: Estanguet Juan Ignacio y Delfino Juan

- ***Creacion del Diagrama de Clases del proyecto***: Adorno Gabriela Soledad.

- ***Creacion y manejo del repositorio, junto con el backlog***: Ludeña Augusto.
