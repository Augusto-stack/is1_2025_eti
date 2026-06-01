package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

// Importaciones necesarias para la aplicación Spark
import java.util.HashMap; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
import java.util.Map; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).
import java.util.List;
import java.util.ArrayList;
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.mindrot.jbcrypt.BCrypt; // Utilidad para hashear y verificar contraseñas de forma segura.

import com.fasterxml.jackson.databind.ObjectMapper; // Representa un modelo de datos y el nombre de la vista a renderizar.
import com.is1.proyecto.config.DBConfigSingleton; // Motor de plantillas Mustache para Spark.
import com.is1.proyecto.config.SessionManager; // para el helper de la expiracion de sesion
import com.is1.proyecto.models.Teacher; // Para crear mapas de datos (modelos para las plantillas).
import com.is1.proyecto.models.User; // Interfaz Map, utilizada para Map.of() o HashMap.

import spark.ModelAndView; // Clase Singleton para la configuración de la base de datos.
import static spark.Spark.after;
import static spark.Spark.before; // Modelo de ActiveJDBC que representa la tabla 'users'.
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;
import com.is1.proyecto.models.Materia;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Clase principal de la aplicación Spark. Configura las rutas, filtros y el
 * inicio del servidor web.
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR
 * ───────────────────────────────────────
 * 1. Se agrega SessionManager (config/SessionManager.java) que centraliza toda
 * la lógica de expiración de sesión.
 * 2. El filtro `before` ya NO contiene lógica de expiración (estaba
 * incompleto).
 * 3. Cada ruta protegida llama a SessionManager.checkSession(req, res) al
 * inicio.
 * 4. El POST /login usa SessionManager.initSession(...) para crear la sesión de
 * forma estandarizada (incluyendo el timestamp de último acceso).
 * 5. El admin NUNCA expira; alumnos y profesores expiran tras 10 min de
 * inactividad. *
 */
public class App {

    // Instancia estática y final de ObjectMapper para la
    // serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Método principal que se ejecuta al iniciar la aplicación. Aquí se
     * configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones
                    // (por defecto es 4567).

        // Obtener la instancia única del singleton de configuración de la base de
        // datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // --- Filtro 'before' para gestionar la conexión a la base de datos ---
        // Este filtro se ejecuta antes de cada solicitud HTTP.
        before((req, res) -> {
            if (Base.hasConnection()) {
                Base.close();
            }

            try {
                // Abre una conexión a la base de datos utilizando las credenciales del
                // singleton.
                Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                System.out.println(req.url());

                // al comenzar la app ejecuto ese base exec por si no existe, asi no hay que
                // correrlo manualmente

                // USERS
                Base.exec(
                        "CREATE TABLE IF NOT EXISTS users ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "name TEXT NOT NULL UNIQUE,"
                                + "password TEXT NOT NULL,"
                                + "role TEXT NOT NULL DEFAULT 'alumno'," // nuevo
                                + "loginAttempts INTEGER DEFAULT 0," // nuevo
                                + "blocked INTEGER DEFAULT 0" // nuevo
                                + ");");

                // TEACHERS
                Base.exec(
                        "CREATE TABLE IF NOT EXISTS teachers ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "name TEXT NOT NULL,"
                                + "lastName TEXT NOT NULL,"
                                + "dni INTEGER NOT NULL UNIQUE,"
                                + "email TEXT NOT NULL UNIQUE,"
                                + "user_id INTEGER"
                                + ");");

                // nuevo MATERIAS
                Base.exec(
                        "CREATE TABLE IF NOT EXISTS materias ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "nombre TEXT NOT NULL UNIQUE,"
                                + "teacher_teorico_id INTEGER,"
                                + "teacher_practico_id INTEGER,"
                                + "FOREIGN KEY (teacher_teorico_id) REFERENCES teachers(id),"
                                + "FOREIGN KEY (teacher_practico_id) REFERENCES teachers(id)"
                                + ");");

                // Tabla intermedia que relaciona alumno-materia (para inscripciones)
                Base.exec(
                        "CREATE TABLE IF NOT EXISTS inscripciones ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "user_id INTEGER NOT NULL,"
                                + "materia_id INTEGER NOT NULL,"
                                + "calificacion1 REAL DEFAULT NULL," // ← nota 1
                                + "calificacion2 REAL DEFAULT NULL," // ← nota 2
                                + "modificada INTEGER DEFAULT 0,"
                                + "comentario TEXT DEFAULT NULL,"
                                + "FOREIGN KEY (user_id) REFERENCES users(id),"
                                + "FOREIGN KEY (materia_id) REFERENCES materias(id)"
                                + ");");

                // Tabla de quejas recibidas de los alumnos
                Base.exec(
                        "CREATE TABLE IF NOT EXISTS complaints (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "user_id INTEGER NOT NULL," + // Relación con el alumno
                                "title TEXT NOT NULL," +
                                "description TEXT NOT NULL," +
                                "status TEXT DEFAULT 'PENDIENTE'," + // PENDIENTE, EN_REVISION, RESUELTA
                                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                                ");");

                // Crear usuario admin por defecto si no existe(VA A HABER SOLAMENTE 1)
                User admin = User.findFirst("name = ?", "admin");
                if (admin == null) {
                    User newAdmin = new User();
                    newAdmin.set("name", "admin");
                    newAdmin.set("password", BCrypt.hashpw("admin123", BCrypt.gensalt()));
                    newAdmin.set("loginAttempts", 0);
                    newAdmin.set("blocked", 0);
                    newAdmin.set("role", "admin"); // asignamos el rol de admin
                    newAdmin.saveIt();
                    System.out.println("DEBUG: Usuario admin creado por defecto.");
                }

            } catch (Exception e) {

                if (Base.hasConnection()) {
                    Base.close();
                }
                // Si ocurre un error al abrir la conexión, se registra y se detiene la
                // solicitud
                // con un código de estado 500 (Internal Server Error) y un mensaje JSON.
                System.err.println("Error al abrir conexión con ActiveJDBC: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}"
                        + e.getMessage());
            }
        });

        // --- Filtro 'after' para cerrar la conexión a la base de datos ---
        // Este filtro se ejecuta después de que cada solicitud HTTP ha sido procesada.
        after((req, res) -> {
            try {
                // Cierra la conexión a la base de datos para liberar recursos.
                Base.close();
            } catch (Exception e) {
                // Si ocurre un error al cerrar la conexión, se registra.
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });

        // GET: renderiza formularios y páginas HTML ---
        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query
        // parameters.
        get("/user/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.
            // Obtener y añadir mensaje de éxito de los query parameters (ej.
            // ?message=Cuenta creada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos
            // vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            // Renderiza la plantilla 'user_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            // Si no está logueado o la sesión expiró, SessionManager redirige al login.
            if (!SessionManager.checkSession(req, res))
                return null;

            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");

            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
                // Redirige al login con un mensaje de error.
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión para acceder a esta página.",
                        StandardCharsets.UTF_8));
                return null; // Importante retornar null después de una redirección.
            }

            // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la
            // plantilla.
            model.put("username", currentUsername);

            // NUEVO
            // 3. Filtramos que tipo de rol tiene el usuario, para mostrar dashboard
            // distintos
            // si es un profesor, alumno o admin.
            String userRole = req.session().attribute("userRole");
            model.put("role", userRole);
            model.put("isAdmin", userRole != null && userRole.equals("admin"));
            model.put("isProfesor", userRole != null && userRole.equals("profesor"));
            model.put("isAlumno", userRole != null && userRole.equals("alumno"));
            // NUEVO

            // Calcular tiempo restante de sesión para alumnos y profesores.
            // El admin nunca expira, por lo que no se setea sessionExpira para él.
            // Mustache interpreta sessionExpira como false si no está seteado.
            if (!"admin".equals(userRole)) {
                Long lastAccess = req.session().attribute("lastAccessTime");
                if (lastAccess != null) {
                    long transcurridos = (System.currentTimeMillis() - lastAccess) / 1000;
                    long restantes = SessionManager.TIMEOUT_SECONDS - transcurridos;
                    model.put("sessionExpira", true);
                    model.put("sessionTiempoSegundos", Math.max(restantes, 0));
                }
            }

            // Mensajes de éxito/error pasados como query parameters (ej. ?message=... o
            // ?error=...)
            String successMsg = req.queryParams("message");
            if (successMsg != null && !successMsg.isEmpty())
                model.put("message", successMsg);
            String errorMsg = req.queryParams("error");
            if (errorMsg != null && !errorMsg.isEmpty())
                model.put("error", errorMsg);

            // 4. Renderiza la plantilla del dashboard con el nombre de usuario.
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para mostrar el formulario de login
        get("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());

        // NUEVO
        // GET: Panel de admin para ver usuarios y desbloquear cuentas
        get("/admin/usuarios", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;

            // (NUEVO)
            if (!esAdmin(req)) {
                res.redirect("/login?error="
                        + URLEncoder.encode("No tenés permisos para acceder a esta página.", StandardCharsets.UTF_8));
                return null;
            }
            // (NUEVO)
            // Obtener todos los usuarios
            List<User> usuarios = User.findAll().load();
            List<Map<String, Object>> listaUsuarios = new ArrayList<>(); // Creamos una lista vacía donde vamos a
                                                                         // guardar los datos de cada usuario
                                                                         // en un formato que Mustache pueda leer.

            for (User u : usuarios) {
                Map<String, Object> usuarioMap = new HashMap<>();
                usuarioMap.put("nombre", u.getString("name")); // Por cada usuario convertimos sus datos a un mapa con
                                                               // tres campos:
                usuarioMap.put("bloqueado", u.getInteger("blocked") == 1);
                usuarioMap.put("intentos", u.getInteger("loginAttempts"));
                usuarioMap.put("rol", u.getString("role"));
                usuarioMap.put("esAdmin", "admin".equals(u.getString("role")));
                usuarioMap.put("esProfesor", "profesor".equals(u.getString("role")));
                usuarioMap.put("esAlumno", "alumno".equals(u.getString("role")));
                listaUsuarios.add(usuarioMap);
            }

            model.put("usuarios", listaUsuarios); // Mandamos la lista al mustache para que la muestre en pantalla.
            return new ModelAndView(model, "admin_usuarios.mustache");

        }, new MustacheTemplateEngine());
        // NUEVO...

        // Nuevo
        get("/alumno/materias", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"alumno".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return null;
            }

            Object userId = req.session().attribute("userId");

            List<Map<String, Object>> listaMaterias = new ArrayList<>();
            for (Materia m : Materia.findAll().<Materia>load()) {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", m.getId());
                mapa.put("nombre", m.getString("nombre"));

                Object teoricoId = m.get("teacher_teorico_id");
                if (teoricoId != null) {
                    Teacher t = Teacher.findById(teoricoId);
                    if (t != null)
                        mapa.put("profesorTeorico", t.getString("name") + " " + t.getString("lastName"));
                } else {
                    mapa.put("profesorTeorico", "Sin asignar");
                }

                Object practicoId = m.get("teacher_practico_id");
                if (practicoId != null) {
                    Teacher t = Teacher.findById(practicoId);
                    if (t != null)
                        mapa.put("profesorPractico", t.getString("name") + " " + t.getString("lastName"));
                } else {
                    mapa.put("profesorPractico", "Sin asignar");
                }

                long yaInscripto = Base.count("inscripciones", "user_id = ? AND materia_id = ?", userId, m.getId());
                mapa.put("inscripto", yaInscripto > 0);
                mapa.put("cantidadAlumnos", Base.count("inscripciones", "materia_id = ?", m.getId()));

                listaMaterias.add(mapa);
            }

            model.put("materias", listaMaterias);

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty())
                model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty())
                model.put("errorMessage", errorMessage);

            return new ModelAndView(model, "alumno_materias.mustache");
        }, new MustacheTemplateEngine()); // ← cierre del GET

        // Nuevo
        // POST: Inscribirse a una materia ← ahora está AFUERA del GET
        post("/alumno/inscribirse/:id", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"alumno".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return "";
            }

            Object userId = req.session().attribute("userId");
            int materiaId = Integer.parseInt(req.params(":id"));

            Materia mat = Materia.findById(materiaId);
            if (mat == null) {
                res.redirect("/alumno/materias?error="
                        + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return "";
            }

            long yaInscripto = Base.count("inscripciones", "user_id = ? AND materia_id = ?", userId, materiaId);
            if (yaInscripto > 0) {
                res.redirect("/alumno/materias?error="
                        + URLEncoder.encode("Ya estás inscripto en esa materia.", StandardCharsets.UTF_8));
                return "";
            }

            Base.exec("INSERT INTO inscripciones (user_id, materia_id) VALUES (?, ?)", userId, materiaId);
            res.redirect("/alumno/materias?message="
                    + URLEncoder.encode("Te inscribiste correctamente.", StandardCharsets.UTF_8));
            return "";
        });

        // Nuevo
        // POST: Desinscribirse de una materia ← también AFUERA del GET
        post("/alumno/desinscribirse/:id", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"alumno".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return "";
            }

            Object userId = req.session().attribute("userId");
            int materiaId = Integer.parseInt(req.params(":id"));

            Base.exec("DELETE FROM inscripciones WHERE user_id = ? AND materia_id = ?", userId, materiaId);
            res.redirect("/alumno/materias?message="
                    + URLEncoder.encode("Te desinscribiste correctamente.", StandardCharsets.UTF_8));
            return "";
        });

        get("/alumno/notas", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"alumno".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return null;
            }

            Object userId = req.session().attribute("userId");

            // Obtener todas las inscripciones del alumno
            List<Map<String, Object>> listaNotas = new ArrayList<>();
            List<Map> inscripciones = Base.findAll(
                    "SELECT * FROM inscripciones WHERE user_id = ?", userId);

            double suma = 0;
            int conNota = 0;

            for (Map inscripcion : inscripciones) {
                Object materiaId = inscripcion.get("materia_id");
                Materia materia = Materia.findById(materiaId);

                if (materia != null) {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("materia", materia.getString("nombre"));

                    // ← cambiamos calificacion por calificacion1 y calificacion2
                    Object cal1 = inscripcion.get("calificacion1");
                    Object cal2 = inscripcion.get("calificacion2");
                    Object modificada = inscripcion.get("modificada");
                    Object comentario = inscripcion.get("comentario");

                    mapa.put("calificacion1", cal1 != null ? cal1 : "Pendiente");
                    mapa.put("calificacion2", cal2 != null ? cal2 : "Pendiente");

                    // Promedio solo si tiene las dos notas
                    if (cal1 != null && cal2 != null) {
                        double promedio = (Double.parseDouble(cal1.toString()) + Double.parseDouble(cal2.toString()))
                                / 2;
                        mapa.put("promedio", String.format("%.2f", promedio));
                        mapa.put("tienePromedio", true);
                        mapa.put("aprobado", promedio >= 6);
                        mapa.put("desaprobado", promedio < 6);
                        suma += promedio;
                        conNota++;
                    } else {
                        mapa.put("tienePromedio", false);
                    }

                    mapa.put("modificada", modificada != null && modificada.toString().equals("1"));
                    mapa.put("comentario", comentario != null ? comentario : "");

                    listaNotas.add(mapa);
                }
            }

            // Promedio general — solo considera materias con las dos notas cargadas
            if (conNota > 0) {
                double promedioGeneral = suma / conNota;
                model.put("promedio", String.format("%.2f", promedioGeneral));
                model.put("tienePromedio", true);
            }

            model.put("notas", listaNotas);
            return new ModelAndView(model, "alumno_notas.mustache");
        }, new MustacheTemplateEngine());

        // Nuevo
        get("/profesor/materias", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"profesor".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return null;
            }

            Object userId = req.session().attribute("userId");

            // Buscar el teacher que corresponde al usuario logueado
            Teacher miTeacher = Teacher.findFirst("user_id = ?", userId);

            List<Map<String, Object>> listaMaterias = new ArrayList<>();

            if (miTeacher != null) {
                for (Materia m : Materia.findAll().<Materia>load()) {
                    Object teoricoId = m.get("teacher_teorico_id");
                    Object practicoId = m.get("teacher_practico_id");

                    boolean esTeorico = teoricoId != null && teoricoId.toString().equals(miTeacher.getId().toString());
                    boolean esPractico = practicoId != null
                            && practicoId.toString().equals(miTeacher.getId().toString());

                    if (esTeorico || esPractico) {
                        Map<String, Object> mapa = new HashMap<>();
                        mapa.put("id", m.getId());
                        mapa.put("nombre", m.getString("nombre"));
                        mapa.put("esTeorico", esTeorico);
                        mapa.put("esPractico", esPractico);
                        mapa.put("cantidadAlumnos", Base.count("inscripciones", "materia_id = ?", m.getId()));
                        listaMaterias.add(mapa);
                    }
                }
            }

            model.put("materias", listaMaterias);

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty())
                model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty())
                model.put("errorMessage", errorMessage);

            return new ModelAndView(model, "profesor_materias.mustache");
        }, new MustacheTemplateEngine());

        // Para que el profesor vea los inscriptos en su mate
        get("/profesor/materias/:id/alumnos", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"profesor".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return null;
            }

            int materiaId = Integer.parseInt(req.params(":id"));
            Materia materia = Materia.findById(materiaId);

            if (materia == null) {
                res.redirect("/profesor/materias?error="
                        + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("materiaNombre", materia.getString("nombre"));
            model.put("materiaId", materiaId);

            // Obtener alumnos inscriptos en esta materia
            List<Map<String, Object>> listaAlumnos = new ArrayList<>();
            List<Map> inscripciones = Base.findAll("SELECT * FROM inscripciones WHERE materia_id = ?", materiaId);

            for (Map inscripcion : inscripciones) {
                Object userId = inscripcion.get("user_id");
                User alumno = User.findById(userId);
                if (alumno != null) {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("nombre", alumno.getString("name"));

                    Object cal1 = inscripcion.get("calificacion1");
                    Object cal2 = inscripcion.get("calificacion2");

                    mapa.put("calificacion1", cal1 != null ? cal1 : "Sin nota");
                    mapa.put("calificacion2", cal2 != null ? cal2 : "Sin nota");

                    // Promedio de la materia para este alumno
                    if (cal1 != null && cal2 != null) {
                        double promedio = (Double.parseDouble(cal1.toString()) + Double.parseDouble(cal2.toString()))
                                / 2;
                        mapa.put("promedio", String.format("%.2f", promedio));
                    } else {
                        mapa.put("promedio", "Incompleto");
                    }

                    listaAlumnos.add(mapa);
                }
            }

            model.put("alumnos", listaAlumnos);

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty())
                model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty())
                model.put("errorMessage", errorMessage);

            return new ModelAndView(model, "profesor_alumnos.mustache");
        }, new MustacheTemplateEngine());

        post("/profesor/materias/:id/nota", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            String userRole = req.session().attribute("userRole");
            if (loggedIn == null || !loggedIn || !"profesor".equals(userRole)) {
                res.redirect("/login?error=" + URLEncoder.encode("No tenés permisos.", StandardCharsets.UTF_8));
                return "";
            }

            int materiaId = Integer.parseInt(req.params(":id"));
            String alumnoNombre = req.queryParams("alumnoNombre");
            String nota1Str = req.queryParams("nota1");
            String nota2Str = req.queryParams("nota2");
            String comentario = req.queryParams("comentario");

            if (alumnoNombre == null || alumnoNombre.isEmpty()) {
                res.redirect("/profesor/materias/" + materiaId + "/alumnos?error="
                        + URLEncoder.encode("Datos incompletos.", StandardCharsets.UTF_8));
                return "";
            }

            User alumno = User.findFirst("name = ?", alumnoNombre);
            if (alumno == null) {
                res.redirect("/profesor/materias/" + materiaId + "/alumnos?error="
                        + URLEncoder.encode("Alumno no encontrado.", StandardCharsets.UTF_8));
                return "";
            }

            // Validar notas si fueron ingresadas
            Double nota1 = null;
            Double nota2 = null;

            if (nota1Str != null && !nota1Str.isEmpty()) {
                nota1 = Double.parseDouble(nota1Str);
                if (nota1 < 0 || nota1 > 10) {
                    res.redirect("/profesor/materias/" + materiaId + "/alumnos?error="
                            + URLEncoder.encode("La nota 1 debe estar entre 0 y 10.", StandardCharsets.UTF_8));
                    return "";
                }
            }
            if (nota2Str != null && !nota2Str.isEmpty()) {
                nota2 = Double.parseDouble(nota2Str);
                if (nota2 < 0 || nota2 > 10) {
                    res.redirect("/profesor/materias/" + materiaId + "/alumnos?error="
                            + URLEncoder.encode("La nota 2 debe estar entre 0 y 10.", StandardCharsets.UTF_8));
                    return "";
                }
            }

            // Verificar si ya tenía alguna nota cargada
            List<Map> existente = Base.findAll(
                    "SELECT * FROM inscripciones WHERE user_id = ? AND materia_id = ?",
                    alumno.getId(), materiaId);

            boolean yaTeniaNota = !existente.isEmpty()
                    && (existente.get(0).get("calificacion1") != null
                            || existente.get(0).get("calificacion2") != null);

            // Construir el UPDATE dinámicamente según qué notas se ingresaron
            if (nota1 != null && nota2 != null) {
                Base.exec(
                        "UPDATE inscripciones SET calificacion1 = ?, calificacion2 = ?, modificada = ?, comentario = ? WHERE user_id = ? AND materia_id = ?",
                        nota1, nota2, yaTeniaNota ? 1 : 0, comentario, alumno.getId(), materiaId);
            } else if (nota1 != null) {
                Base.exec(
                        "UPDATE inscripciones SET calificacion1 = ?, modificada = ?, comentario = ? WHERE user_id = ? AND materia_id = ?",
                        nota1, yaTeniaNota ? 1 : 0, comentario, alumno.getId(), materiaId);
            } else if (nota2 != null) {
                Base.exec(
                        "UPDATE inscripciones SET calificacion2 = ?, modificada = ?, comentario = ? WHERE user_id = ? AND materia_id = ?",
                        nota2, yaTeniaNota ? 1 : 0, comentario, alumno.getId(), materiaId);
            } else {
                res.redirect("/profesor/materias/" + materiaId + "/alumnos?error="
                        + URLEncoder.encode("Ingresá al menos una nota.", StandardCharsets.UTF_8));
                return "";
            }

            // Actualizar promedio_materia si tiene las dos notas
            if (nota1 != null && nota2 != null) {
                double promedio = (nota1 + nota2) / 2;
                Base.exec(
                        "UPDATE inscripciones SET promedio_materia = ? WHERE user_id = ? AND materia_id = ?",
                        promedio, alumno.getId(), materiaId);
            }

            res.redirect("/profesor/materias/" + materiaId + "/alumnos?message="
                    + URLEncoder.encode("Nota cargada correctamente.", StandardCharsets.UTF_8));
            return "";
        });

        // Nuevo
        // GET: Ver todas las materias
        get("/admin/materias", (req, res) -> {

            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;

            if (!esAdmin(req)) {
                res.redirect("/login?error=No tenés permisos.");
                return null;
            }
            Map<String, Object> model = new HashMap<>();

            List<Map<String, Object>> listaMaterias = new ArrayList<>();
            for (Materia m : Materia.findAll().<Materia>load()) {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", m.getId());
                mapa.put("nombre", m.getString("nombre"));

                // Profesor teórico
                Object teoricoId = m.get("teacher_teorico_id");
                if (teoricoId != null) {
                    Teacher t = Teacher.findById(teoricoId);
                    if (t != null)
                        mapa.put("profesorTeorico", t.getString("name") + " " + t.getString("lastName"));
                } else {
                    mapa.put("profesorTeorico", "Sin asignar");
                }

                // Profesor práctico
                Object practicoId = m.get("teacher_practico_id");
                if (practicoId != null) {
                    Teacher t = Teacher.findById(practicoId);
                    if (t != null)
                        mapa.put("profesorPractico", t.getString("name") + " " + t.getString("lastName"));
                } else {
                    mapa.put("profesorPractico", "Sin asignar");
                }

                // Cantidad de inscriptos
                long cantidad = Base.count("inscripciones", "materia_id = ?", m.getId());
                mapa.put("cantidadAlumnos", cantidad);

                listaMaterias.add(mapa);
            }

            // Cargar profesores para los selectores
            List<Map<String, Object>> listaProfesores = new ArrayList<>();
            for (Teacher t : Teacher.findAll().<Teacher>load()) {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", t.getId());
                mapa.put("nombreCompleto", t.getString("name") + " " + t.getString("lastName"));
                listaProfesores.add(mapa);
            }

            model.put("materias", listaMaterias);
            model.put("profesores", listaProfesores);

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty())
                model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty())
                model.put("errorMessage", errorMessage);

            return new ModelAndView(model, "admin_materias.mustache");
        }, new MustacheTemplateEngine());

        // POST: Crear materia
        post("/admin/materias/crear", (req, res) -> {
            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;

            if (!esAdmin(req)) {
                res.redirect("/login?error=No tenés permisos.");
                return "";
            }

            String nombre = req.queryParams("nombre");
            String teoricoIdStr = req.queryParams("teacher_teorico_id");
            String practicoIdStr = req.queryParams("teacher_practico_id");

            if (nombre == null || nombre.isEmpty()) {
                res.redirect("/admin/materias?error=El nombre es requerido.");
                return "";
            }

            Materia existe = Materia.findFirst("nombre = ?", nombre);
            if (existe != null) {
                res.redirect("/admin/materias?error=Ya existe una materia con ese nombre.");
                return "";
            }

            Materia mat = new Materia();
            mat.set("nombre", nombre);
            if (teoricoIdStr != null && !teoricoIdStr.isEmpty()) {
                mat.set("teacher_teorico_id", Integer.parseInt(teoricoIdStr));
            }
            if (practicoIdStr != null && !practicoIdStr.isEmpty()) {
                mat.set("teacher_practico_id", Integer.parseInt(practicoIdStr));
            }
            mat.saveIt();

            res.redirect("/admin/materias?message="
                    + URLEncoder.encode("Materia '" + nombre + "' creada correctamente.", StandardCharsets.UTF_8));
            return "";
        });

        // POST: Eliminar materia
        post("/admin/materias/eliminar/:id", (req, res) -> {
            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;
            if (!esAdmin(req)) {
                res.redirect("/login?error=No tenés permisos.");
                return "";
            }

            int id = Integer.parseInt(req.params(":id"));
            Materia mat = Materia.findById(id);

            if (mat == null) {
                res.redirect("/admin/materias?error=Materia no encontrada.");
                return "";
            }

            Base.exec("DELETE FROM inscripciones WHERE materia_id = ?", id);
            mat.delete();

            res.redirect("/admin/materias?message="
                    + URLEncoder.encode("Materia eliminada correctamente.", StandardCharsets.UTF_8));
            return "";
        });

        // NUEVO...
        // POST: Desbloquear cuenta de usuario
        post("/admin/desbloquear/:nombre", (req, res) -> {
            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;

            // Lo colocamos por si alguien supiera la URL para desbloquear usuarios.
            if (!esAdmin(req)) {
                res.redirect("/login?error=No tenés permisos.");
                return "";
            }

            String nombre = req.params(":nombre"); // Recibe el nombre del usurio bloqueado desde la URL

            User u = User.findFirst("name = ?", nombre); // Lo busca en la BD

            if (u == null) {
                res.redirect("/admin/usuarios?error=Usuario no encontrado.");
                return "";
            }

            u.set("blocked", 0); // Resetea el blocked a 0
            u.set("loginAttempts", 0); // Resetea los intenos a 0
            u.saveIt();

            res.redirect("/admin/usuarios?message="
                    + URLEncoder.encode("Usuario " + nombre + " desbloqueado exitosamente.", StandardCharsets.UTF_8));
            return "";
        });
        // NUEVO...

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como
            // inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para
            // invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de
        // los query params
        // si se la usa como destino de redirecciones. (Tu código de /user/create ya lo
        // hace, aplicar similar).

        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta de alias para el formulario de creación de cuenta.
        // En una aplicación real, probablemente querrías unificar con '/user/create'
        // para evitar duplicidad.

        get("/user/new", (req, res) -> {
            return new ModelAndView(new HashMap<>(), "user_form.mustache"); // No pasa un modelo específico, solo el
                                                                            // formulario.
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        /// SECCION DE PROFESORES
        // GET para el manejo del envio de formulario de carga de profesor

        get("/cargarProfesor", (req, res) -> {
            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;

            Map<String, Object> model = new HashMap<>();

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            return new ModelAndView(model, "teacher_formulario.mustache");
        }, new MustacheTemplateEngine());

        // GET: Formulario para que el alumno redacte su queja
        get("/alumno/queja", (req, res) -> {
            if (!esAlumno(req)) {
                res.redirect("/dashboard?error=Solo los alumnos pueden enviar quejas.");
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("username", req.session().attribute("currentUserUsername"));
            return new ModelAndView(model, "queja_form.mustache");
        }, new MustacheTemplateEngine());

        // GET: Vista para que el ADMIN vea todas las quejas enviadas por los alumnos
        // --- ESTA ES LA ÚNICA QUE DEBE QUEDAR ---
        get("/admin/quejas", (req, res) -> {
            if (!esAdmin(req)) {
                res.redirect("/dashboard?error=" + encode("No tenés permisos."));
                return null;
            }
            Map<String, Object> model = new HashMap<>();

            // Capturamos el mensaje de la URL para que el Admin vea "Queja resuelta" arriba
            String successMsg = req.queryParams("message");
            if (successMsg != null)
                model.put("message", successMsg);

            List<Map<String, Object>> listaQuejas = new ArrayList<>();

            // Traemos las quejas
            List<Map> quejasRaw = Base.findAll(
                    "SELECT c.*, u.name as alumno_nombre FROM complaints c JOIN users u ON c.user_id = u.id ORDER BY c.created_at DESC");

            for (Map q : quejasRaw) {
                Map<String, Object> quejaMap = new HashMap<>(q);

                // Esta es la lógica que separa las listas en el HTML
                quejaMap.put("esResuelta", "RESUELTA".equals(q.get("status")));

                listaQuejas.add(quejaMap);
            }

            model.put("quejas", listaQuejas);
            // IMPORTANTE: Que el nombre termine en 's' (admin_quejas.mustache)
            return new ModelAndView(model, "admin_quejas.mustache");
        }, new MustacheTemplateEngine());
        // POST de los profesores, envia el formulario con el alta de los datos del
        // nuevo profesor

        post("/cargarProfesor", (req, res) -> {
            // NUEVO 4/4: Verificar sesión activa y expiración por inactividad.
            if (!SessionManager.checkSession(req, res))
                return null;

            // obtengo datos
            String name = req.queryParams("name");
            String lastname = req.queryParams("lastName");
            String email = req.queryParams("email");
            String dniStr = req.queryParams("dni");
            String username = req.queryParams("username"); // (NUEVO)
            String password = req.queryParams("password"); // (NUEVO)

            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (name == null || name.isEmpty() || lastname == null || lastname.isEmpty()
                    || email == null || email.isEmpty()
                    || dniStr == null || dniStr.isEmpty()
                    || username == null || username.isEmpty() // ← nuevo
                    || password == null || password.isEmpty()) { // ← nuevo
                res.redirect("/cargarProfesor?error="
                        + URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8));
                return "";
            }

            int dni = Integer.parseInt(dniStr);

            // compruebo existencia de dni
            Teacher dniExiste = Teacher.findFirst("dni = ?", dni);
            if (dniExiste != null) {
                res.status(409);
                res.redirect("/cargarProfesor?error=" + URLEncoder
                        .encode("El dni del docente '" + dni + "' ya esta en uso.", StandardCharsets.UTF_8));
                return "";
            }

            // compruebo el email
            Teacher emailExiste = Teacher.findFirst("email = ?", email);

            if (emailExiste != null) {
                res.status(409);
                res.redirect("/cargarProfesor?error=" + URLEncoder.encode(
                        "El email del docente '" + email + "' ya esta en uso. Elige otro.", StandardCharsets.UTF_8));
                return "";
            }

            // Verificar que no haya nombre de usuario igual en la BD (NUEVO)
            User usernameExiste = User.findFirst("name = ?", username);
            if (usernameExiste != null) {
                res.redirect("/cargarProfesor?error=" + URLEncoder
                        .encode("El nombre de usuario '" + username + "' ya está en uso.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Guardamos al profesor
                Teacher teacher = new Teacher();
                teacher.set("name", name);
                teacher.set("lastName", lastname);
                teacher.set("dni", dni);
                teacher.set("email", email);
                teacher.saveIt();

                // Creamos las credenciales del profesor (NUEVO)
                User nuevoUser = new User();
                nuevoUser.set("name", username);
                nuevoUser.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
                nuevoUser.set("role", "profesor");
                nuevoUser.set("loginAttempts", 0);
                nuevoUser.set("blocked", 0);
                nuevoUser.saveIt();

                // Vinculamos el teacher con el user ← nuevo
                teacher.set("user_id", nuevoUser.getId());
                teacher.saveIt();

                res.status(201);
                res.redirect("/cargarProfesor?message=" + URLEncoder.encode(
                        "El docente " + name + " " + lastname + " se ingreso correctamente!", StandardCharsets.UTF_8));

                return "";

            } catch (Exception e) {
                System.err.println("Error en el alta del nuevo profesor: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                res.redirect("/cargarProfesor?error=Error interno al crear la cuenta. Intente de nuevo.");
                return "";
            }
        });

        // --- Rutas POST para manejar envíos de formularios y APIs ---
        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", (req, res) -> {
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400); // Código de estado HTTP 400 (Bad Request).
                // Redirige al formulario de creación con un mensaje de error.
                res.redirect("/user/create?error=Nombre y contraseña son requeridos.");
                return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
            }

            try {
                // Intenta crear y guardar la nueva cuenta en la base de datos.
                User ac = new User(); // Crea una nueva instancia del modelo User.
                // Hashea la contraseña de forma segura antes de guardarla.
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                ac.set("name", name); // Asigna el nombre de usuario.
                ac.set("password", hashedPassword); // Asigna la contraseña hasheada.
                ac.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                res.redirect("/user/create?message="
                        + URLEncoder.encode("Cuenta creada exitosamente para " + name + "!", StandardCharsets.UTF_8));
                return ""; // Retorna una cadena vacía.

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario
                // duplicado),
                // se captura aquí y se redirige con un mensaje de error.
                System.err.println("Error al registrar la cuenta: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Código de estado HTTP 500 (Internal Server Error).
                res.redirect("/user/create?error=Error interno al crear la cuenta. Intente de nuevo.");
                return ""; // Retorna una cadena vacía.
            }
        });

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla de login o dashboard.

            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");

            // Validaciones básicas: campos de usuario y contraseña no pueden ser nulos o
            // vacíos.
            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.status(400); // Bad Request.
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }

            // Busca la cuenta en la base de datos por el nombre de usuario.
            User ac = User.findFirst("name = ?", username);

            // Si no se encuentra ninguna cuenta con ese nombre de usuario.
            if (ac == null) {
                res.status(401);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                return new ModelAndView(model, "login.mustache");
            }

            // Verificar si la cuenta está bloqueada NUEVO
            int blocked = ac.getInteger("blocked");
            // variable para que no se bloquee al admin
            String roleCheck = ac.getString("role");
            if (blocked == 1 && !"admin".equals(roleCheck)) {
                res.status(401);
                model.put("errorMessage", "Tu cuenta está bloqueada, contactá al administrador.");
                return new ModelAndView(model, "login.mustache"); // NUEVO
            }

            // Obtiene la contraseña hasheada almacenada en la base de datos.
            String storedHashedPassword = ac.getString("password");

            // Compara la contraseña en texto plano ingresada con la contraseña hasheada
            // almacenada.
            // BCrypt.checkpw hashea la plainTextPassword con el salt de
            // storedHashedPassword y compara.
            if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
                // Autenticación exitosa.
                res.status(200); // OK.

                // --- Gestión de Sesión ---
                ac.set("loginAttempts", 0); // NUEVO
                ac.set("blocked", 0);
                ac.saveIt(); // NUEVO

                req.session(true).attribute("currentUserUsername", username); // Guarda el nombre de usuario en la
                                                                              // sesión.
                req.session().attribute("userId", ac.getId()); // Guarda el ID de la cuenta en la sesión (útil).
                req.session().attribute("loggedIn", true); // Establece una bandera para indicar que el usuario está
                                                           // logueado.
                String userRole = ac.getString("role"); // guardamos el rol en una variable (NUEVO)
                req.session().attribute("userRole", userRole); // guardamos el valor en la session (NUEVO)

                // NUEVO 4/4: Inicializar sesión a través de SessionManager.
                // Guarda todos los atributos de sesión y el timestamp de último acceso.
                SessionManager.initSession(req, username, ac.getId(), userRole);

                System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
                System.out.println("DEBUG: ID de Sesión: " + req.session().id());

                model.put("username", username); // Añade el nombre de usuario al modelo para el dashboard.
                // Renderiza la plantilla del dashboard tras un login exitoso.
                res.redirect("/dashboard");
                return null;
            } else {
                // variable para que no se bloquee al admin
                String userRole = ac.getString("role");
                if ("admin".equals(userRole)) {
                    res.status(401);
                    model.put("errorMessage", "Usuario o contraseña incorrectos.");
                    return new ModelAndView(model, "login.mustache");
                }
                // Sumar 1 al contador de intentos fallidos NUEVO
                int attempts = ac.getInteger("loginAttempts") + 1;
                ac.set("loginAttempts", attempts);

                // Si llegó a 3 intentos, bloquear la cuenta
                if (attempts >= 3) {
                    ac.set("blocked", 1);
                    ac.saveIt();
                    res.status(401);
                    model.put("errorMessage",
                            "Tu cuenta fue bloqueada por demasiados intentos fallidos. Contactá al administrador.");
                    return new ModelAndView(model, "login.mustache");
                }

                ac.saveIt();
                res.status(401);
                System.out.println("DEBUG: Intento de login fallido para: " + username);
                model.put("errorMessage", "Usuario o contraseña incorrectos. Intentos restantes: " + (3 - attempts));
                return new ModelAndView(model, "login.mustache"); // NUEVO
            }
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta POST.

        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", (req, res) -> {
            res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

            // Obtiene los parámetros 'name' y 'password' de la solicitud.
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // --- Validaciones básicas ---
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
            }

            try {
                // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
                User newUser = new User(); // Crea una nueva instancia de tu modelo User.
                newUser.set("name", name); // Asigna el nombre al campo 'name'.
                newUser.set("password", password); // Asigna la contraseña al campo 'password'.
                newUser.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Created.
                // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
                return objectMapper.writeValueAsString(
                        Map.of("message", "Usuario '" + name + "' registrado con éxito.", "id", newUser.getId()));

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB, se captura aquí.
                System.err.println("Error al registrar usuario: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Internal Server Error.
                return objectMapper
                        .writeValueAsString(Map.of("error", "Error interno al registrar usuario: " + e.getMessage()));
            }
        });

        // Procesar el envío de la queja
        post("/alumno/queja", (req, res) -> {
            // 1. Verificación de seguridad
            if (!esAlumno(req)) {
                res.status(403);
                return "Acceso denegado";
            }

            // 2. Obtención de datos del formulario
            String title = req.queryParams("title");
            String description = req.queryParams("description");
            Object userId = req.session().attribute("userId"); // Obtenemos el ID del alumno de la sesión

            // 3. Validación de campos vacíos
            if (title == null || title.isEmpty() || description == null || description.isEmpty()) {
                res.redirect("/alumno/queja?error="
                        + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // 4. Inserción en la base de datos
                // Usamos Base.exec directamente para asegurar que los datos entren
                // correctamente
                Base.exec("INSERT INTO complaints (user_id, title, description, status) VALUES (?, ?, ?, ?)",
                        userId, title, description, "PENDIENTE");

                System.out.println("DEBUG: Queja enviada por el usuario ID: " + userId);

                res.redirect("/dashboard?message="
                        + URLEncoder.encode("Queja enviada correctamente.", StandardCharsets.UTF_8));
                return "";
            } catch (Exception e) {
                System.err.println("Error al insertar queja: " + e.getMessage());
                e.printStackTrace();
                res.redirect("/alumno/queja?error="
                        + URLEncoder.encode("Error interno al procesar la queja.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/admin/quejas", (req, res) -> {
            if (!esAdmin(req)) {
                res.redirect("/dashboard?error=" + encode("No tenés permisos."));
                return null;
            }
            Map<String, Object> model = new HashMap<>();

            List<Map<String, Object>> listaQuejas = new ArrayList<>();

            // 1. Buscamos todas las quejas de la base de datos
            List<Map> quejasRaw = Base.findAll(
                    "SELECT c.*, u.name as alumno_nombre FROM complaints c JOIN users u ON c.user_id = u.id ORDER BY c.created_at DESC");

            for (Map q : quejasRaw) {
                // 2. Creamos un nuevo mapa para poder agregarle datos extra
                Map<String, Object> quejaMap = new HashMap<>(q);

                // 3. AQUÍ VA EL AJUSTE LÓGICO:
                // Creamos la "bandera" esResuelta que Mustache usará para filtrar
                quejaMap.put("esResuelta", "RESUELTA".equals(q.get("status")));

                listaQuejas.add(quejaMap);
            }

            model.put("quejas", listaQuejas);
            return new ModelAndView(model, "admin_quejas.mustache");
        }, new MustacheTemplateEngine());

        // POST: Cambiar estado de la queja a EN_REVISION
        post("/admin/quejas/revisar/:id", (req, res) -> {
            if (!esAdmin(req))
                return "";
            Base.exec("UPDATE complaints SET status = 'EN_REVISION' WHERE id = ?", req.params(":id"));
            res.redirect("/admin/quejas?message=" + encode("Queja puesta en revisión."));
            return "";
        });

        // POST: Cambiar estado de la queja a RESUELTA
        post("/admin/quejas/resolver/:id", (req, res) -> {
            if (!esAdmin(req))
                return "";
            Base.exec("UPDATE complaints SET status = 'RESUELTA' WHERE id = ?", req.params(":id"));
            res.redirect("/admin/quejas?message=" + encode("Queja marcada como resuelta."));
            return "";
        });

        // GET: Panel de estadísticas generales del sistema.
        // Solo accesible para el administrador.
        // Muestra conteos de usuarios, materias, inscripciones, quejas y destacados.
        get("/admin/estadisticas", (req, res) -> {

            // Verificar sesión activa
            if (!SessionManager.checkSession(req, res))
                return null;

            // Solo el admin puede acceder
            if (!esAdmin(req)) {
                res.redirect("/dashboard?error=" + encode("No tenés permisos."));
                return null;
            }

            Map<String, Object> model = new HashMap<>();

            // --- Conteos generales ---
            long totalAlumnos = Base.count("users", "role = ?", "alumno");
            long totalProfesores = Base.count("users", "role = ?", "profesor");
            long totalMaterias = Base.count("materias");
            long totalInscripciones = Base.count("inscripciones");

            // --- Quejas agrupadas por estado ---
            long quejasPendientes = Base.count("complaints", "status = ?", "PENDIENTE");
            long quejasEnRevision = Base.count("complaints", "status = ?", "EN_REVISION");
            long quejasResueltas = Base.count("complaints", "status = ?", "RESUELTA");
            long quejasTotal = quejasPendientes + quejasEnRevision + quejasResueltas;

            // --- Alumnos bloqueados por intentos fallidos ---
            long alumnosBloqueados = Base.count("users", "blocked = 1 AND role = ?", "alumno");

            // --- Materia con mayor cantidad de inscriptos ---
            List<Map> topMateria = Base.findAll(
                    "SELECT m.nombre, COUNT(i.id) as cantidad " +
                            "FROM materias m LEFT JOIN inscripciones i ON m.id = i.materia_id " +
                            "GROUP BY m.id ORDER BY cantidad DESC LIMIT 1");
            if (!topMateria.isEmpty()) {
                model.put("topMateriaNombre", topMateria.get(0).get("nombre"));
                model.put("topMateriaCantidad", topMateria.get(0).get("cantidad"));
                model.put("hayTopMateria", true);
            }

            // --- Alumno con mejor promedio de calificaciones ---
            List<Map> topAlumno = Base.findAll(
                    "SELECT u.name, AVG(i.calificacion) as promedio " +
                            "FROM users u JOIN inscripciones i ON u.id = i.user_id " +
                            "WHERE i.calificacion IS NOT NULL AND u.role = 'alumno' " +
                            "GROUP BY u.id ORDER BY promedio DESC LIMIT 1");
            if (!topAlumno.isEmpty()) {
                model.put("topAlumnoNombre", topAlumno.get(0).get("name"));
                Object prom = topAlumno.get(0).get("promedio");
                model.put("topAlumnoPromedio",
                        prom != null ? String.format("%.2f", Double.parseDouble(prom.toString())) : "-");
                model.put("hayTopAlumno", true);
            }

            // --- Pasar todos los datos al modelo para Mustache ---
            model.put("totalAlumnos", totalAlumnos);
            model.put("totalProfesores", totalProfesores);
            model.put("totalMaterias", totalMaterias);
            model.put("totalInscripciones", totalInscripciones);
            model.put("quejasPendientes", quejasPendientes);
            model.put("quejasEnRevision", quejasEnRevision);
            model.put("quejasResueltas", quejasResueltas);
            model.put("quejasTotal", quejasTotal);
            model.put("alumnosBloqueados", alumnosBloqueados);

            return new ModelAndView(model, "admin_estadisticas.mustache");
        }, new MustacheTemplateEngine());
    } // Fin del método main

    // Creamos este metodo para ahorrarnos el verificar si es admin o no.
    private static boolean esAdmin(spark.Request req) {
        Boolean loggedIn = req.session().attribute("loggedIn");
        String role = req.session().attribute("userRole");
        return loggedIn != null && loggedIn && "admin".equals(role);
    }

    // Creamos este metodo para ahorrarnos el verificar si es alumno o no.
    private static boolean esAlumno(spark.Request req) {
        Boolean loggedIn = req.session().attribute("loggedIn");
        String role = req.session().attribute("userRole");
        return loggedIn != null && loggedIn && "alumno".equals(role);
    }

    // Método auxiliar para codificar mensajes en la URL y evitar errores de
    // sintaxis
    private static String encode(String msg) {
        if (msg == null)
            return "";
        return java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
    }

} // Fin de la clase App
