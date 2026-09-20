# CasitaGO — Backend

Backend del proyecto de curso **Marketplace de Alojamientos** (Inversiones LR),
Ingeniería de Software — Universidad El Bosque.

## Stack

- Java 21 + Spring Boot 3.3 (Maven)
- PostgreSQL (Supabase) — esquema administrado manualmente (sin Flyway/Liquibase)
- Spring Security + JWT
- springdoc-openapi (contratos de API / Swagger)

## Arquitectura

Backend 100% API REST (patrón MVC de Spring: Controller → Service → Repository
→ Entity, y DTO como lo que efectivamente viaja en el JSON) consumido por un
frontend Angular separado. Estructura de paquetes **plana** por capa, no por
módulo:

```
co.edu.unbosque.casitago
├── controller/   (todos los @RestController)
├── service/      (toda la lógica de negocio)
├── repository/   (todas las interfaces Spring Data JPA)
├── entity/       (todas las entidades JPA)
├── dto/          (todos los DTO de entrada/salida)
├── common/
│   ├── exception/  (manejo global de errores)
│   └── audit/      (utilidad para registrar eventos de auditoría)
└── config/       (seguridad, JWT, OpenAPI, etc.)
```

Como no hay carpetas separadas por servicio, la trazabilidad hacia el diseño
orientado a servicios (documentado en Confluence) se mantiene por
**convención de nombres de clase**:

| Prefijo | RF que atiende |
|---|---|
| `Auth*` | RF-01 a RF-07, RF-25 |
| `Listing*` | RF-08 a RF-12, RF-24 |
| `Availability*` | RF-13, RF-14 |
| `Search*` | RF-15, RF-16 |
| `Booking*` | RF-17 a RF-20 |
| `Payment*` | RF-35 |
| `Notification*` | RF-21, RF-34 |
| `Review*` | RF-22, RF-23 |
| `Admin*` | RF-24 a RF-26 |
| `Reporting*` | RF-27, RF-28 |

## Base de datos

El esquema completo está en `database/schema.sql`. Se ejecuta **manualmente**
una vez en el SQL Editor de Supabase (Project → SQL Editor → New query → pegar
→ Run). Si más adelante hay que modificar una tabla, se agrega un archivo
nuevo (`database/V2__algo.sql`, etc.) con el `ALTER` correspondiente, para
dejar un historial legible — pero nadie lo ejecuta automáticamente, lo corres
tú a mano en Supabase cada vez.

`spring.jpa.hibernate.ddl-auto` está en `validate`: Hibernate revisa que las
entidades `@Entity` coincidan con las tablas ya creadas y falla rápido si no
coinciden, pero nunca crea ni altera tablas por su cuenta.

## Cómo correr el proyecto localmente

1. Copia `.env.example` a `.env` y completa los valores reales de tu proyecto
   Supabase (o configura las variables directamente en IntelliJ, ver abajo).
2. Asegúrate de haber corrido `database/schema.sql` en Supabase al menos una vez.
3. Ejecuta:
   ```bash
   ./mvnw spring-boot:run
   ```
4. La API queda disponible en `http://localhost:8080`.
5. Documentación interactiva (Swagger UI): `http://localhost:8080/docs`.

## Cómo correr las pruebas

```bash
./mvnw test
```

## Estado actual

Esqueleto inicial: estructura de paquetes, configuración de conexión a base
de datos, script de esquema completo. La implementación de cada módulo se
hace de forma incremental, siguiendo el orden: `auth` → `listings` →
`availability` → `search` → `booking` + `payments` → `notifications` →
`reviews` → `admin` + `reporting`.
