# Resumen del Proyecto: Aplicacion Movil de Notas

## Que se hizo

Se desarrollo una aplicacion movil sencilla para gestionar notas. El proyecto esta dividido en dos partes:

- `notes-backend`: API REST creada con Java 21 y Spring Boot.
- `notes-android`: aplicacion Android que consume el backend y guarda notas localmente.

La aplicacion permite registrar usuarios, iniciar sesion, crear notas, consultar notas, editarlas, eliminarlas y conservar informacion localmente cuando no hay conexion.

## Tecnologias usadas

### Backend

- Java 21.
- Spring Boot.
- Spring Web.
- Spring Security.
- Spring Data JPA.
- MySQL.
- JWT para autenticacion.
- Maven.

### Android

- Android API 36.
- Java.
- Retrofit para consumir el API REST.
- Room para persistencia local con SQLite.
- SharedPreferences para guardar el token JWT.
- Gradle.

## Backend

El backend se encarga de administrar usuarios y notas. Expone endpoints REST para que la aplicacion Android pueda comunicarse con el servidor.

Endpoints principales:

- `POST /api/auth/register`: registrar usuario.
- `POST /api/auth/login`: iniciar sesion.
- `GET /api/notes`: listar notas.
- `GET /api/notes/{id}`: consultar una nota.
- `POST /api/notes`: crear nota.
- `PUT /api/notes/{id}`: actualizar nota.
- `DELETE /api/notes/{id}`: eliminar nota.
- `GET /api/notes/sync`: sincronizar notas desde una fecha.

La base de datos usada es MySQL. Las tablas se crean automaticamente cuando inicia el backend gracias a esta configuracion:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Aplicacion Android

La aplicacion Android tiene una pantalla sencilla donde el usuario puede registrarse o iniciar sesion. Despues de autenticarse, puede administrar sus notas.

La app consume el backend usando Retrofit. La URL del API se configura en:

```properties
API_BASE_URL=http://10.0.2.2:8080/
```

Esa URL sirve para probar desde un emulador Android. Si se usa un celular fisico, se debe cambiar por la IP local del computador.

## Persistencia local

Para cumplir con el modo offline se uso Room, que trabaja sobre SQLite. Las notas se guardan en una base de datos local dentro del dispositivo.

Cuando no hay conexion:

- La app muestra las notas guardadas localmente.
- Las notas nuevas o modificadas quedan marcadas como pendientes de sincronizar.
- Las eliminaciones tambien se guardan localmente como pendientes.

Cuando vuelve la conexion:

- La app envia al servidor las notas pendientes.
- Luego consulta los cambios del servidor.
- Actualiza la base local con la informacion mas reciente.

## Autenticacion

Se uso JWT porque es un mecanismo simple y adecuado para una aplicacion movil. Cuando el usuario inicia sesion o se registra, el backend devuelve un token.

La app guarda ese token en `SharedPreferences` y lo envia en cada peticion protegida:

```http
Authorization: Bearer <token>
```

De esta forma, solo los usuarios autenticados pueden acceder a sus notas.

## Decisiones tecnicas

- Se eligio Spring Boot porque permite crear rapidamente un API REST ordenado y conectado a MySQL.
- Se uso MySQL porque era el motor de base de datos requerido.
- Se uso JPA/Hibernate para que el backend cree las tablas automaticamente al iniciar.
- Se uso JWT para evitar manejar sesiones en el servidor.
- Se uso Room en Android porque es una forma recomendada y estable de trabajar con SQLite.
- Se uso Retrofit porque simplifica el consumo de endpoints REST desde Android.
- Se agrego un endpoint de sincronizacion para que la app pueda comparar cambios usando fechas.
- Se agregaron campos `createdAt`, `updatedAt` y `deletedAt` para facilitar la sincronizacion offline.
- Se implemento borrado logico en el backend para que la app pueda enterarse de notas eliminadas durante la sincronizacion.
- Se mantuvo una arquitectura sencilla por capas para que el proyecto sea facil de entender y explicar.

## Arquitectura general

### Backend

- `model`: entidades de base de datos.
- `repository`: consultas con Spring Data JPA.
- `service`: logica de negocio.
- `controller`: endpoints REST.
- `dto`: datos de entrada y salida.
- `security`: configuracion JWT.
- `exception`: manejo de errores.

### Android

- `MainActivity`: pantalla principal y acciones del usuario.
- `api`: clases para Retrofit y DTOs.
- `data`: Room, DAO y entidad local.
- `repository`: logica para decidir entre servidor remoto y base local.

## Resultado

El resultado es una aplicacion movil funcional conectada a un backend REST. Cumple con autenticacion, consumo de API, persistencia local y sincronizacion basica para modo offline.
