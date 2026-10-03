# Notes Backend

Backend REST para una aplicacion movil de notas con Java 21, Spring Boot, MySQL y autenticacion JWT.

## Arquitectura

El proyecto usa una arquitectura basica por capas, cercana a MVC para API REST:

- `model`: entidades JPA (`AppUser`, `Note`).
- `controller`: endpoints REST.
- `service`: reglas simples de negocio.
- `repository`: acceso a base de datos con Spring Data JPA.
- `dto`: objetos de entrada y salida JSON.
- `security`: JWT y configuracion de Spring Security.
- `exception`: manejo simple de errores.

Como es un API REST, no hay vistas HTML. La respuesta JSON de cada controlador cumple el papel de salida hacia la app movil.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL 8+

## Configuracion

La aplicacion crea o actualiza las tablas al iniciar usando Hibernate:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Variables de entorno disponibles:

| Variable | Valor por defecto |
| --- | --- |
| `SERVER_PORT` | `8080` |
| `DB_URL` | `jdbc:mysql://localhost:3306/notes_app?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | vacio |
| `JWT_SECRET` | secreto de desarrollo incluido en `application.properties` |
| `JWT_EXPIRATION_MINUTES` | `120` |

Para produccion, cambia siempre `JWT_SECRET` por un valor largo y privado.

## Ejecutar

```bash
mvn spring-boot:run
```

Si Maven no esta en el PATH, ejecuta el comando desde tu IDE o instala/configura Maven.

## Endpoints

### Autenticacion

Registrar usuario:

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "ana@example.com",
  "name": "Ana",
  "password": "secreto123"
}
```

Iniciar sesion:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "ana@example.com",
  "password": "secreto123"
}
```

La respuesta entrega un token JWT. Usalo en los endpoints de notas:

```http
Authorization: Bearer <accessToken>
```

### Notas

```http
GET /api/notes
GET /api/notes/{id}
POST /api/notes
PUT /api/notes/{id}
DELETE /api/notes/{id}
GET /api/notes/sync?updatedAfter=2026-09-05T12:00:00Z
```

Crear o actualizar nota:

```json
{
  "title": "Comprar leche",
  "content": "Pasar por el supermercado"
}
```

## Decisiones tecnicas

### Persistencia local en la app movil

El backend esta preparado para trabajar con una app movil que use SQLite, Room o almacenamiento interno. Cada nota tiene `createdAt`, `updatedAt` y `deletedAt`. Con esos campos, la app puede guardar notas localmente sin internet y sincronizarlas despues.

### Consumo del API REST

El API expone operaciones CRUD con JSON sobre HTTP. `GET /api/notes` devuelve notas activas. `GET /api/notes/sync` devuelve cambios desde una fecha para apoyar el modo offline.

### Autenticacion

Se usa JWT firmado con HMAC porque es simple para una app movil y no requiere sesiones en servidor. Spring Security valida el token en cada request y cada consulta filtra por el `userId` autenticado, evitando que un usuario vea notas de otro.
