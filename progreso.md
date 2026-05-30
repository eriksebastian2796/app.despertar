# Progreso del Proyecto: Despertar

## 1. Persistencia de Datos (Room & KSP)
*   **Base de Datos Unificada:** Creación de `AppDatabase` (v3) para centralizar la información.
*   **Módulo de Favoritos:** Implementación de `FavoriteApp` para persistencia de apps en el Home Screen.
*   **Módulo de Alarmas:** Creación de `AlarmEntity` con soporte para:
    *   Días de repetición (mediante `TypeConverter`).
    *   Tipos de desafío (Matemáticas, Código de barras, Fotos).
    *   Dificultad y cantidad de problemas.
*   **Configuración de Sueño:** Implementación de `SleepConfigEntity` para gestionar horas de descanso y filtro de luz cálida.

## 2. Lógica y Reactividad (ViewModels)
*   **LauncherViewModel:** Transformación reactiva. Observa la DB y reconstruye información de apps (nombres/íconos) automáticamente.
*   **AlarmViewModel:** Lógica para gestión de alarmas, cálculo dinámico de horas de sueño y programación automática en el sistema.
*   **Optimización:** Uso de `PackageManager` eficiente para recuperación de recursos.

## 3. Interfaz de Usuario (Jetpack Compose)
*   **Home Screen (Launcher):** Solución de errores de renderizado y sincronización con el botón "Home".
*   **Alarm Screen:** Interfaz moderna con:
    *   Card de **"Hora de Sueño"** con cálculo de descanso.
    *   Lista de alarmas con contador dinámico "Suena en X h Y m" que se actualiza cada 30 segundos.
*   **Edición de Alarma:** Implementación de **Drum Picker** (tambor circular infinito) para selección de hora y minutos, y selector de dificultad para desafíos.
*   **Alarm Challenge:** Pantalla de desafío matemático con **múltiple choice** (4 opciones grandes), efectos visuales de error/acierto y vibración.

## 4. Infraestructura y Motor de Alarma
*   **Motor de Disparo:** Implementación de `AlarmScheduler` con `setAlarmClock()` para precisión absoluta, incluso en modo Doze y dispositivos antiguos (ZTE API 29).
*   **Servicio de Alarma:** `AlarmService` para reproducción de sonido en loop y notificación persistente de alta prioridad.
*   **Gestión de Permisos:** Detección de optimización de batería con dialog informativo y manejo inteligente de `SCHEDULE_EXACT_ALARM` según versión de Android.
*   **Visibilidad:** Configuración de `AlarmChallengeActivity` para mostrarse sobre la pantalla de bloqueo.
*   **GitHub:** Proyecto vinculado y sincronizado en `https://github.com/eriksebastian2796/app.despertar.git`.

---
*Última actualización: 28 de mayo de 2026*
