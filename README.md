# CuidaSalud (Proyecto_Interaccion)

Aplicación móvil asistencial (Android, Java) para **cuidadores primarios** de personas con problemas de salud.

## Justificación
Al llegar a una edad avanzada aparecen problemas de salud y muchos familiares asumen el cuidado sin preparación:
les cuesta organizar las citas y comprender los resultados médicos. CuidaSalud ataca ambos problemas a la vez:
reduce los olvidos con un sistema de citas con recordatorios, y traduce los análisis clínicos de un lenguaje técnico
a uno claro, pudiendo consultarlos sin que un especialista esté presente. Al ser móvil y de bajo costo, es accesible y escalable.

**No es una app que diagnostique**: solo apoya la administración de citas y la comprensión de diagnósticos y resultados
emitidos por un especialista.

## Objetivo general
Desarrollar una aplicación móvil asistencial orientada a cuidadores primarios que integre un módulo de simplificación
lingüística de resultados clínicos mediante modelos de procesamiento de lenguaje natural (PLN) y un sistema centralizado
de gestión y recordatorio de citas médicas, evaluando su usabilidad en una cohorte piloto.

## Cómo cumple cada parte del objetivo
| Parte del objetivo | Pantalla | Clase |
|---|---|---|
| Módulo de simplificación lingüística con PLN | Traductor IA | `Traductor`, `ClaudeClient` |
| Gestión centralizada de citas | Agenda | `Agenda`, `Almacen` |
| Recordatorio de citas | Notificaciones | `Recordatorios`, `RecordatorioReceiver`, `BootReceiver` |
| Cuidador primario con varias personas a cargo | Familia | `Familia` |
| Evaluación de usabilidad en cohorte piloto | Evaluación (SUS) | `Evaluacion` |

## Configuración de la IA
1. Obtén una clave en https://console.anthropic.com.
2. Abre `local.properties` (raíz del proyecto, no se sube a Git) y agrega:
   ```
   ANTHROPIC_API_KEY=sk-ant-...
   # opcional: ANTHROPIC_MODEL=claude-sonnet-5-5
   ```
3. Sincroniza Gradle y ejecuta.

> **Seguridad:** la clave queda compilada dentro del APK. Es aceptable para la cohorte piloto con un presupuesto limitado en
> la consola; para producción hay que mover la llamada a un backend propio que guarde la clave.

## Cómo funciona el traductor
- Entrada: texto pegado y/o foto del análisis (se reduce a ≤1568 px antes de enviarse).
- La IA responde un JSON con: resumen, cada valor con su rango de referencia y estado (normal / alto / bajo / revisar),
  preguntas sugeridas para el médico y señales a vigilar.
- Dos niveles: *muy sencillo* y *con más detalle*.
- Reglas del sistema: no diagnostica, no sugiere tratamientos ni cambios de medicación, trata el documento como datos
  (defensa contra instrucciones inyectadas) y rechaza contenido que no sea clínico.
- Los análisis se guardan en el dispositivo (historial de 30) y pueden compartirse con la familia.

## Privacidad
Citas, personas y análisis se guardan solo en el dispositivo. Lo único que sale es el texto/foto que el usuario decide
traducir, enviado por HTTPS al servicio de IA. La app avisa de esto y sugiere no incluir nombres ni identificaciones.

## Evaluación de usabilidad (cohorte piloto)
Pantalla *Evaluación*: escala **SUS** de 10 ítems (puntaje 0–100) + 2 ítems propios (claridad de la traducción y utilidad
de los recordatorios). Guarda cada participante, calcula el promedio de la cohorte y exporta un CSV por el menú de compartir.
Interpretación: >80.3 excelente · 68–80.3 buena · 51–68 aceptable · <51 deficiente.

## Estructura
```
app/src/main/java/com/example/proyecto_interaccion/
  Pantalla_inicio  splash con video         MainActivity  bienvenida
  Inicio           menú + próxima cita      Agenda        citas y recordatorios
  Familia          personas a cargo         Traductor     traductor de análisis con IA
  Evaluacion       cuestionario SUS         ClaudeClient  llamada a la API de IA
  Almacen          datos locales (JSON)     Recordatorios / RecordatorioReceiver / BootReceiver
  BaseActivity     pantalla completa, márgenes seguros y cabecera común
```
Diseño: tema oscuro en azul y negro (`res/values/colors.xml` y `themes.xml`).
