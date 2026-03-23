![](unrc_logo.png)

**Departamento de Computación**  
**Asignatura: Ingeniería de Software II (Cód. 3387)**  
**Año 2026**

# Guia de Trabajo Practico

## 1. (Requirements) Describir su proyecto:


### a) Problemas propuestos por nuestro equipo a resolver.

- Poner un limite de intentos a 3 en el login de Usuario.
- Colocar una via para recuperar contraseña.
- Nuevos roles: *alumno*, *profesor*, *admin*.
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

 El tamaño de nuestro equipo va a ser de 5 personas
