![](unrc_logo.png)

**Departamento de Computación**  
**Asignatura: Ingeniería de Software II (Cód. 3387)**  
**Año 2026**

# Guia de Trabajo Practico

---

## 1. INTRODUCCION
    En el presente documento se explican y analisan los requerimientos del proyecto SIUA *(Sistema Integral Universitario Academico)* desarrollado para la materia Ingenieria del Sotware II de la carrera de Analista en Computación.

### 1.1 Proposito
    Este documento tiene como finalidad dar a conocer el funcionamiento general del proyecto SIUA *(Sistema Integral Universitario Academico)*, que esta dirigido al proyecto desarrollado para la materia Ingenieria del Software II como continuacion del proyecto iniciado en la materia correlativa Ingenieria del Software I.

#### 1.2 Contexto del Sistema
    -Nombre del Sistema: SIUA *(Sistema Integral Universitario Academico)* //el nombre del sistema queda sujeto a cambios acordado por el grupo //
    -El sistema esta destinado a la gestion de procesos administrativos y academicos de una institucion educativa. Se espera hacer la alta y baja de profesores, alumnos y personal administrativo (con el rol de administradores), como asi tambien el alta y baja de materias de carreras.
    -El principal beneficiario del sistema seran las instituciones que lo implementen. El principal objetivo es sistematizar y optimizar los procesos en tiempos y recursos.

#### 1.3 Usuarios a los que esta dirigido
    El sistema esta dirigido principalmente a tres diferentes usuarios:
- ***Administrador*** : Conformado por personal administrativo de la institucion que deberan cargar, editar y eliminar cuentas de los profesores o estudiantes que sean parte de la institucion como asi la carga, edicion y baja de materias. Tambien deberan coordinar los profesores y materias respectivas de las diferentes areas/carreras/facultades. Por ultimo seran los responsables de adminisrar las el reseteo de contraseñas en las cuentas de Profesores y alimnos en casos de problemas con las mismas.
- ***Profesores*** : Podran tener un usuario, administrar la informacion (cargar, editar y/o eliminacion) de las materias a la que esten asociados (pueden ser mas de 1), podran acceder al listado de alumnos inscriptos dichas materias asociadas y administrar datos (cargar, editar y/o eliminar) que tengan que ver con los alumnos en relacion a las materias (asignacion de taareas, notas).
- ***Alumnos*** : Los alumnos podran darse de alta en una materia a la que deban cursar o que deban rendir (inscripcion) como asi tambien podran darse de baja a dichas inscripciones. Tendran acceso a la informacion de la materia para consultar notas o tareas cargadas por el profesor que administre la misma.

#### 1.4 Funcionalidades Principales

-Gestion de usuarios: administrativos (alta, modificacion y baja).
- Gestion de usuarios: profesores (alta, modificacion y baja).
- Gestion de usuarios: estudiantes (alta, modificacion y baja).
- Poner un limite de intentos a 3 en el login de Usuario. (HECHO)
- Colocar una via para recuperar contraseña.              (DUDA)
- Nuevos roles: *alumno*, *profesor*, *admin*.              (HECHO)
- Gestion de materias: ***asignatuara*** (alta, modificacion y baja). (Nombre de la asignatura y codigo, profesor responsable, calificaciones, estudiantes inscriptos, cantidad estudiantes).
- Filtros en la creacion de cuentas.
- Cada rol vea un dashboard distinto.
- Generacion de una Base de Datos adecuada.

#### 1.5 Tecnologias Elegidas.

El proyecto será desarrollado principalmente utilizando el lenguaje **Java**, apoyándose en el framework **Spark Framework** para la construcción de la capa web.
Para la implementación de las vistas se empleará el motor de plantillas **Mustache**, permitiendo la generación dinámica de contenido HTML.
En cuanto a la persistencia de datos, se utilizará una base de datos liviana **SQLite**, adecuada para entornos de desarrollo y pruebas. La interacción con la base de datos se gestionará mediante el framework **ActiveJDBC**, facilitando el acceso y manipulación de datos a través del patrón Active Record.

#### 1.6 Restricciones Tecnicas

-**Lenguaje y Framework**: El sistema deberá ser desarrollado utilizando el lenguaje **Java** y el framework **Spark Framework**, limitando la elección de tecnologías backend a este entorno.
- **Motor de Base de Datos**: Se utilizará **SQLite** como sistema de gestión de base de datos, lo cual implica restricciones en cuanto a concurrencia y escalabilidad en comparación con motores más robustos (como ser PostgreSQL o MySQL).
- **Acceso a Datos**: La interacción con la base de datos deberá realizarse mediante el framework **ActiveJDBC**, siguiendo el patrón Active Record.
- **Gestión de Concurrencia**: Debido a las limitaciones de SQLite, el sistema deberá restringir la cantidad de conexiones concurrentes para evitar bloqueos o degradación del rendimiento.
- **Arquitectura Web**: El sistema deberá implementar una arquitectura basada en servidor web liviano, utilizando Spark, sin el uso de frameworks empresariales más pesados como Spring.
- **Motor de Vistas**: Las vistas deberán desarrollarse utilizando **Mustache**, lo cual implica que la lógica de presentación debe mantenerse separada de la lógica de negocio.
- **Limitaciones de Seguridad**:
- Se deberá implementar un límite de intentos de login (máximo 3 intentos).
- El sistema deberá contemplar un mecanismo de recuperación de contraseña.
- **Despliegue**: El sistema deberá ser ejecutable en entornos locales sin requerir configuraciones complejas de infraestructura externa.
- **Escalabilidad**: El sistema está orientado a un entorno académico de pequeña o mediana escala, por lo que no se contemplan soluciones distribuidas ni balanceo de carga en esta etapa.
- **Compatibilidad**: El sistema deberá ser accesible mediante navegadores web modernos (Chrome, Firefox, Edge, entre otros).

---

### d) Tamaño del equipo y Plazo estimado

 El tamaño de nuestro equipo va a ser de 5 personas y el plazo que creemos que nos va a llevar van a ser 2 meses.

 ### e) Cambio de alcanse ocurridos
