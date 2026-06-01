## 2. Análisis de riesgos

### a) Riesgos identificados por IA

| Tipo de Riesgo     | Descripción                                                                                                                           | Probabilidad | Impacto |
| :----------------- | :------------------------------------------------------------------------------------------------------------------------------------ | :----------- | :------ |
| **Técnico**        | Curva de aprendizaje en Spark Framework y ActiveJDBC.                                                                                 | Alta         | Alto    |
| **Técnico**        | SQLite no soporta concurrencia — si dos usuarios operan al mismo tiempo pueden generarse conflictos de conexión.                      | Media        | Alto    |
| **Técnico**        | Mustache es un motor de plantillas muy limitado, puede dificultar vistas complejas por rol.                                           | Media        | Medio   |
| **Técnico**        | Al correr localmente en el puerto 8080, cualquier cambio de entorno puede romper la app.                                              | Baja         | Medio   |
| **Organizacional** | El equipo tiene roles definidos pero tareas como documentación y codificación recaen sobre las mismas personas, generando sobrecarga. | Media        | Alto    |
| **Organizacional** | Desalineación en la prioridad de tareas entre integrantes.                                                                            | Media        | Medio   |
| **Planificación**  | El plazo de 2 meses puede ser insuficiente dado el volumen de funcionalidades planificadas.                                           | Alta         | Alto    |
| **Planificación**  | Desviación en la estimación de funcionalidades complejas como inscripción a materias, notas y estadísticas.                           | Alta         | Crítico |
| **Humano**         | Falta de disponibilidad de un integrante clave en momentos críticos del desarrollo.                                                   | Media        | Alto    |
| **Humano**         | Diferencias en el nivel de experiencia técnica entre integrantes puede generar dependencia en uno o dos miembros.                     | Alta         | Alto    |
| **Seguridad**      | El reseteo de contraseñas lo hace el admin manualmente, si la cuenta del admin se compromete todo el sistema queda expuesto.          | Baja         | Crítico |
| **Seguridad**      | Las sesiones de Spark no tienen expiración configurada explícitamente, lo que puede dejar sesiones abiertas indefinidamente.          | Media        | Medio   |

### b) Riesgos identificados por el equipo

| Tipo de Riesgo     | Descripción                                                                                                                                                                                                                                                                                   | Probabilidad | Impacto |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ------- |
| Técnico            | Dificultad para implementar el sistema de recuperación de contraseña por email, lo que obligó a cambiar el enfoque inicialmente previsto.                                                                                                                                                     | Alta         | Medio   |
| Planificación      | Subestimación del tiempo necesario para implementar la entidad Materia y las funcionalidades asociadas.                                                                                                                                                                                       | Alta         | Alto    |
| Técnico            | Complejidad para diseñar y ejecutar pruebas que contemplen correctamente todos los roles del sistema (administrador, docente y estudiante), pudiendo dejar errores sin detectar.                                                                                                              | Media        | Alto    |
| Seguridad / Diseño | El sistema no cuenta con mecanismos de validación que garanticen la unicidad de las cuentas de usuario, permitiendo la creación de múltiples cuentas para una misma persona y afectando la integridad y trazabilidad de la información.                                                       | Media        | Alto    |
| Seguridad / Diseño | El mecanismo de creación de usuarios no restringe adecuadamente la asignación de roles, permitiendo que usuarios generen cuentas con privilegios de docente o administrador. Esto podría comprometer la integridad, confidencialidad y seguridad de la información gestionada por el sistema. | Media        | Crítico |

### c) Comparación

### Comparación de ambos análisis

**Riesgos que encontró la IA y el equipo no:**
La IA identificó riesgos relacionados con la organización del equipo, la planificación general del proyecto, limitaciones tecnológicas (SQLite, Mustache, Spark), dependencia de integrantes clave y problemas de gestión de sesiones y contraseñas.

**Riesgos que encontró el equipo y la IA no:**
El equipo identificó riesgos surgidos durante el desarrollo, como la complejidad del testing entre distintos roles, la posibilidad de cuentas duplicadas y la asignación incorrecta de privilegios a usuarios.

**Calidad del análisis:**
El análisis de la IA es más amplio y abarca aspectos técnicos, organizacionales y de planificación. El análisis del equipo es más específico y está enfocado en problemas reales detectados durante la implementación del sistema. Ambos enfoques son complementarios y permiten obtener una visión más completa de los riesgos del proyecto.
