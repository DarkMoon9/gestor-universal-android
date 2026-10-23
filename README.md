# Gestor Universal de Android

Aplicación de escritorio desarrollada en Java Swing para la administración de dispositivos móviles Android y emuladores mediante el protocolo ADB (Android Debug Bridge) y la herramienta de transmisión de pantalla Scrcpy.

---

## Funcionalidades Principal

* Detección de Dispositivos y Emuladores: Identificación de equipos físicos conectados por USB y autodetector de emuladores activos (Android Studio AVD, MuMu Player, BlueStacks, NoxPlayer, LDPlayer, MEmu y WSA) mediante sondeo de puertos TCP en red local.
* Integración con Scrcpy: Transmisión de pantalla a 60 FPS con sistema de descarga y descompresión dinámica del ejecutable portable para entornos donde no está preinstalado.
* Administración de Aplicaciones:
    * Extracción asíncrona de iconos originales desde paquetes APK (incluyendo soporte para Split APKs).
    * Sistema de caché en dos niveles (memoria RAM y persistencia en disco local bajo la carpeta `cache_iconos/`).
    * Generación de avatares gráficos vectoriales de respaldo para aplicaciones con recursos basados exclusivamente en XML adaptativo.
    * Filtro de búsqueda en tiempo real por nombre de aplicación e identificador de paquete.
* Instalación mediante Arrastrar y Soltar (Drag and Drop): Soporte nativo para soltar archivos .apk sobre la interfaz gráfica para iniciar el proceso de instalación automática.
* Consola de Registros: Monitoreo en tiempo real de comandos ADB, salidas del sistema y estados de conexión.

---

## Requisitos del Sistema

* Java Development Kit (JDK) 17 o superior.
* Apache Maven 3.8 o superior.
* Servidor Android Debug Bridge (ADB) configurado en el sistema o accesible ejecutablemente.

---

## Estructura del Proyecto

El código fuente sigue una arquitectura organizada por responsabilidades en el paquete `com.rsec.appaware`:

* `model`: Clases de datos del dominio (`Dispositivo`, `AppInfo`).
* `service`: Lógica de integración con procesos externos del sistema operativo y comandos ADB (`ADBService`).
* `ui`: Ventanas principales, diálogos modales y fábricas de componentes (`GestorApp`, `UIFactory`).
* `ui.components`: Renderizadores visuales personalizados para listas y selectores (`DispositivoRenderer`, `AppListRenderer`).

---

## Compilación y Ejecución

### 1. Clonar el repositorio

```bash
git clone [https://github.com/DarkMoon9/gestor-universal-android.git](https://github.com/DarkMoon9/gestor-universal-android.git)
cd gestor-universal-android