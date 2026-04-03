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

#### 1.5 Restricciones Tecnicas y Tecnologias Elegidas.



### a) Problemas propuestos por nuestro equipo a resolver.

- Poner un limite de intentos a 3 en el login de Usuario. (HECHO)
- Colocar una via para recuperar contraseña.              (DUDA)
- Nuevos roles: *alumno*, *profesor*, *admin*.              (HECHO)
- Nueva entidad: **Asignatura** (nombre, profesor a cargo, calificacion, estudiantes inscriptos, cantidad estudiantes).
- Filtros en la creacion de cuentas.
- Cada rol vea un dashboard distinto.           

--- 

### b) Usuarios y  Funcionalidades.

Tendriamos 3 tipos de usuarios:

- ***Alumnos*** : Las funcionalidades que van a tener son las de inscribirse a las materias, ver sus notas y sus materias disponibles.

- ***Profesores*** : Sus funcionalidades serian ver las materias que tiene asignada, ver el listado de estudiantes inscriptos en cada una (si tiene mas de una materia asignada), cargar notas y modificarlas si se equivoca.

- ***Administrador*** : El admin puede cargar/editar/eliminar cuentas(profesor estudiante), crear/editar/eliminar materias, asignar un *profesor* a una materia, reseteo de contraseñas.

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

 ### e) Cambio de alcanse ocurridos
