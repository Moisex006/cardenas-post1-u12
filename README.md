# Post-contenido — Unidad 12: Despliegue y CI/CD

## Descripción
Repositorio del laboratorio de la Unidad 12 de Programación Web —
Séptimo Semestre. Contiene la aplicación Spring Boot dockerizada
con un Dockerfile multi-stage y perfiles dev/prod (Parte 1), que se
extenderá con un pipeline de integración continua con GitHub
Actions y monitoreo con Spring Boot Actuator (Parte 2).

**Aplicación base:** el catálogo de productos (`catalogo-productos`) construido en la Unidad 10:
API REST `/api/productos` sobre la entidad JPA `Producto`, con su suite de pruebas
(Mockito, `@DataJpaTest`, `@WebMvcTest`) y la colección Postman en `postman/`.

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/api/productos` | 201 con el producto creado; 400 si el nombre está vacío o el precio ≤ 0 |
| GET | `/api/productos/{id}` | 200 con el producto; 404 si no existe |
| GET | `/api/productos?categoria=X` | 200 con los productos de esa categoría |

## Parte 1 — Dockerización con Perfiles y Variables de Entorno
El Dockerfile multi-stage compila el proyecto con Maven en una
etapa JDK y ejecuta el JAR resultante en una etapa JRE Alpine con
un usuario no root. La configuración sensible se externaliza en
application.properties con placeholders `${VAR:default}`: sin
variables de entorno, la aplicación arranca con el perfil dev
(H2 en memoria); con `SPRING_PROFILES_ACTIVE=prod` y las variables
`DATASOURCE_URL` / `DATASOURCE_USER` / `DATASOURCE_PASSWORD`,
arranca con la configuración de producción (PostgreSQL). Esta aplicación
no usa JWT, por eso no define `JWT_SECRET`.

### Variables de entorno
| Variable | Perfil | Valor por defecto | Uso |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ambos | `dev` | Perfil activo (`dev` o `prod`) |
| `PORT` | ambos | `8080` | Puerto HTTP (Railway y Render lo asignan) |
| `DATASOURCE_URL` | dev / prod | `jdbc:h2:mem:devdb` / `jdbc:postgresql://localhost:5432/appdb` | URL JDBC de la base de datos |
| `DATASOURCE_USER` | dev / prod | `sa` / `appuser` | Usuario de la base de datos |
| `DATASOURCE_PASSWORD` | dev / prod | vacío | Contraseña (en prod siempre se define por variable) |
| `DATASOURCE_DRIVER` | dev | `org.h2.Driver` | Driver JDBC del perfil dev |

## Cómo compilar y ejecutar
1. Compilar sin Docker: `./mvnw clean package -DskipTests` y ejecutar `java -jar target/catalogo-0.0.1-SNAPSHOT.jar`
2. Construir la imagen: `docker build -t mi-spring-app:local .`
3. Ejecutar con los valores por defecto (perfil dev, H2 en memoria):
   ```bash
   docker run -d --name mi-app-dev -p 8080:8080 mi-spring-app:local
   curl http://localhost:8080/api/productos?categoria=ELECTRONICA
   ```
4. Ejecutar con perfil prod contra PostgreSQL (por ejemplo un PostgreSQL local en Docker):
   ```bash
   docker run -d --name pg-appdb -p 5432:5432 -e POSTGRES_DB=appdb \
     -e POSTGRES_USER=appuser -e POSTGRES_PASSWORD=apppass postgres:16-alpine

   docker run -d --name mi-app-prod -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -e DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/appdb \
     -e DATASOURCE_USER=appuser \
     -e DATASOURCE_PASSWORD=apppass \
     mi-spring-app:local

   docker logs mi-app-prod   # debe mostrar: The following 1 profile is active: "prod"
   ```

## Decisiones de diseño
1. Placeholders con valor por defecto (`${VAR:default}`) en vez de
   placeholders estrictos: permite arrancar sin fricción en local
   y en CI, y deja los valores reales para producción.
2. `application-dev.properties` y `application-prod.properties` como
   archivos separados en vez de un único archivo con bloques
   condicionales: hace explícito qué configuración corresponde a
   cada ambiente.
3. Imagen final eclipse-temurin JRE Alpine en vez de distroless:
   reduce el tamaño de la imagen sin sacrificar la posibilidad de
   depurar el contenedor con `docker exec`.
4. Etapa de compilación `maven:3.9-eclipse-temurin-21-alpine`: es el mismo JDK Temurin 21 sobre
   Alpine, pero con Maven incluido; la imagen `eclipse-temurin:21-jdk-alpine` no trae el comando `mvn`.
5. Esquema de producción con `schema-postgresql.sql` (`CREATE TABLE IF NOT EXISTS`): en prod
   `ddl-auto=validate` no crea tablas, así que el esquema se crea con un script que solo se ejecuta
   con el perfil prod (`spring.sql.init.platform=postgresql`) y es seguro en cada reinicio.
