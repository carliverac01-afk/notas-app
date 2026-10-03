# Notes Android

Aplicacion Android sencilla para gestionar notas con autenticacion, consumo de API REST y persistencia local offline.

## Requisitos

- Android Studio
- Android SDK Platform 36
- Backend `notes-backend` corriendo en Spring Boot

## Configuracion del backend

Por defecto la app consume la URL definida en `gradle.properties`:

```text
API_BASE_URL=http://10.0.2.2:8080/
```

Ese host funciona desde el emulador Android para acceder al `localhost` del computador.

Si pruebas desde un celular fisico, cambia `API_BASE_URL` en `gradle.properties` por la IP local de tu computador, por ejemplo:

```properties
API_BASE_URL=http://192.168.1.50:8080/
```

El celular y el computador deben estar en la misma red Wi-Fi.

## Funcionalidad

- Registro de usuario.
- Inicio de sesion con JWT.
- Pantalla principal con nombre y email del usuario autenticado.
- Crear notas.
- Listar notas.
- Editar notas.
- Eliminar notas.
- Guardar notas localmente con Room cuando no hay conexion.
- Sincronizar notas pendientes cuando vuelve la conexion.
- Guardar el token con `EncryptedSharedPreferences`.
- Manejar estado global con `ViewModel`.
- Solicitar permiso de ubicacion solo cuando el usuario usa el boton de ubicacion.
- Consultar ubicacion de forma puntual, sin seguimiento continuo.

## Arquitectura basica

- `MainActivity`: pantalla principal y acciones de usuario.
- `NotesViewModel`: estado global de sesion, usuario, notas y mensajes.
- `api`: DTOs y cliente Retrofit.
- `data`: Room, entidad local y DAO.
- `repository`: logica para decidir entre backend remoto y base local.

## Compilar

```bash
./gradlew :app:assembleDebug
```

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

El APK debug queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```
