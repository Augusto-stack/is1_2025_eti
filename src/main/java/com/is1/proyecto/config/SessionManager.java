package com.is1.proyecto.config;
 
import spark.Request;
import spark.Response;
 
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
 
/**
 * Utilidad para gestionar la expiración de sesión por inactividad.
 *
 * Reglas:
 *  - El admin NUNCA expira.
 *  - Cualquier otro rol (alumno, profesor) expira tras TIMEOUT_SECONDS de inactividad.
 *  - La expiración se evalúa en cada request a rutas protegidas.
 *  - El tiempo de último acceso se actualiza en cada request válido.
 *
 * IMPORTANTE: Este método NO usa halt() para no interrumpir el flujo de Spark.
 * Si halt() se llama dentro de un handler, el filtro 'after' no se ejecuta,
 * y la conexión a la base de datos (abierta en el filtro 'before') nunca se cierra,
 * lo que causa el error: "Cannot open a new connection because existing connection
 * is still on current thread".
 * Por eso checkSession() devuelve un boolean: la ruta que lo llama es responsable
 * de retornar null si la sesión no es válida.
 */
public class SessionManager {
 
    /** Tiempo máximo de inactividad permitido (en segundos). 10 minutos por defecto. */
    public static final long TIMEOUT_SECONDS = 600;
 
    /** Clave del atributo de sesión que guarda el timestamp del último acceso. */
    private static final String LAST_ACCESS_KEY = "lastAccessTime";
 
    /**
     * Verifica si la sesión del usuario actual está autenticada y vigente.
     *
     * Si la sesión no es válida o expiró, redirige al login y devuelve FALSE.
     * Si la sesión es válida, actualiza el timestamp y devuelve TRUE.
     *
     * La ruta que llama a este método DEBE verificar el valor de retorno y
     * hacer "return null" si es false, para que el flujo de Spark llegue
     * correctamente al filtro 'after' que cierra la conexión a la DB.
     *
     * Ejemplo de uso en una ruta:
     *   if (!SessionManager.checkSession(req, res)) return null;
     *
     * @param req Request de Spark
     * @param res Response de Spark
     * @return true si la sesión es válida, false si no lo es (ya redirigió al login)
     */
    public static boolean checkSession(Request req, Response res) {
        Boolean loggedIn = req.session().attribute("loggedIn");
        String username  = req.session().attribute("currentUserUsername");
 
        // 1. ¿Está logueado?
        if (loggedIn == null || !loggedIn || username == null) {
            redirectToLogin(res, "Debes iniciar sesión para acceder a esta página.");
            return false; // La ruta debe hacer "return null" al recibir false.
        }
 
        // 2. El admin no expira nunca.
        String role = req.session().attribute("userRole");
        if ("admin".equals(role)) {
            return true; // Sin restricción de tiempo para el admin.
        }
 
        // 3. Para cualquier otro rol, verificar inactividad.
        Long lastAccess = req.session().attribute(LAST_ACCESS_KEY);
        long now = System.currentTimeMillis();
 
        if (lastAccess != null) {
            long inactiveSeconds = (now - lastAccess) / 1000;
            if (inactiveSeconds > TIMEOUT_SECONDS) {
                System.out.println("DEBUG: Sesión expirada por inactividad para usuario: " + username
                        + " (" + inactiveSeconds + "s sin actividad)");
                req.session().invalidate();
                redirectToLogin(res, "Tu sesión expiró por inactividad. Por favor, iniciá sesión nuevamente.");
                return false; // La ruta debe hacer "return null" al recibir false.
            }
        }
 
        // 4. Sesión vigente → actualizar timestamp de último acceso.
        req.session().attribute(LAST_ACCESS_KEY, now);
        return true; // Sesión válida, la ruta puede continuar.
    }
 
    /**
     * Inicializa los atributos de sesión al momento del login exitoso.
     * Debe llamarse justo después de que las credenciales fueron verificadas.
     *
     * @param req      Request de Spark
     * @param username Nombre de usuario autenticado
     * @param userId   ID del usuario en la base de datos
     * @param role     Rol del usuario (admin / profesor / alumno)
     */
    public static void initSession(Request req, String username, Object userId, String role) {
        req.session(true); // Crea la sesión si no existe (invalida la anterior implícitamente).
        req.session().attribute("currentUserUsername", username);
        req.session().attribute("userId", userId);
        req.session().attribute("loggedIn", true);
        req.session().attribute("userRole", role);
 
        // Solo guardamos lastAccessTime para roles que tienen restricción de tiempo.
        if (!"admin".equals(role)) {
            req.session().attribute(LAST_ACCESS_KEY, System.currentTimeMillis());
        }
 
        System.out.println("DEBUG: Sesión iniciada para '" + username + "' con rol '" + role + "'.");
    }
 
    // ─── Helpers privados ────────────────────────────────────────────────────────
 
    private static void redirectToLogin(Response res, String mensaje) {
        try {
            res.redirect("/login?error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
        } catch (Exception e) {
            res.redirect("/login");
        }
    }
}
