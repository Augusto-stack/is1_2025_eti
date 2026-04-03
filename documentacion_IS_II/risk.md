## 2. Análisis de riesgos

### a) Riesgos identificados por IA

| Tipo de Riesgo     | Descripción                                                                 | Probabilidad | Impacto  |
| :----------------- | :-------------------------------------------------------------------------- | :----------- | :------- |
| **Técnico**        | Curva de aprendizaje en Spark Framework y ActiveJDBC.                       | Alta         | Alto     |
| **Técnico**        | SQLite no soporta concurrencia — si dos usuarios operan al mismo tiempo pueden generarse conflictos de conexión. | Media | Alto |
| **Técnico**        | Mustache es un motor de plantillas muy limitado, puede dificultar vistas complejas por rol. | Media | Medio |
| **Técnico**        | Al correr localmente en el puerto 8080, cualquier cambio de entorno puede romper la app. | Baja | Medio |
| **Organizacional** | El equipo tiene roles definidos pero tareas como documentación y codificación recaen sobre las mismas personas, generando sobrecarga. | Media | Alto |
| **Organizacional** | Desalineación en la prioridad de tareas entre integrantes.                  | Media        | Medio    |
| **Planificación**  | El plazo de 2 meses puede ser insuficiente dado el volumen de funcionalidades planificadas. | Alta | Alto |
| **Planificación**  | Desviación en la estimación de funcionalidades complejas como inscripción a materias, notas y estadísticas. | Alta | Crítico |
| **Humano**         | Falta de disponibilidad de un integrante clave en momentos críticos del desarrollo. | Media | Alto |
| **Humano**         | Diferencias en el nivel de experiencia técnica entre integrantes puede generar dependencia en uno o dos miembros. | Alta | Alto |
| **Seguridad**      | El reseteo de contraseñas lo hace el admin manualmente, si la cuenta del admin se compromete todo el sistema queda expuesto. | Baja | Crítico |
| **Seguridad**      | Las sesiones de Spark no tienen expiración configurada explícitamente, lo que puede dejar sesiones abiertas indefinidamente. | Media | Medio |

### b) Riesgos identificados por el equipo

| Tipo de Riesgo     | Descripción                                                                 | Probabilidad | Impacto  |
| :----------------- | :-------------------------------------------------------------------------- | :----------- | :------- |
| **Técnico**        | Dificultad para implementar el sistema de recuperación de contraseña por email, lo que obligó a cambiar el enfoque. | Alta | Medio |
| **Planificación**  | Subestimación del tiempo necesario para implementar la entidad Materia y todo lo que depende de ella. | Alta | Alto |

### c) Comparación

**Riesgos que encontró la IA y el equipo no:**
- Conflictos de conexión en SQLite por concurrencia.
- Limitaciones de Mustache para vistas complejas.
- Sobrecarga de integrantes con múltiples roles.
- Riesgo de seguridad si la cuenta admin se compromete.
- Sesiones sin expiración explícita.

**Riesgos que encontró el equipo y la IA no:**
- La dificultad concreta con el sistema de recuperación de contraseña por email, que ya derivó en un cambio de alcance real en el proyecto.

**Conclusión:**
El análisis de la IA cubrió riesgos más amplios y transversales (técnicos, 
organizacionales, de seguridad), mientras que el equipo identificó riesgos 
más específicos surgidos de la experiencia directa con el proyecto. 
La combinación de ambos enfoques permite tener una visión más completa 
de los posibles problemas.