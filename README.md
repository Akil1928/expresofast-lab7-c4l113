# ExpresoFast - Consola de Operación Logística

Proyecto del curso IF0009 - Desarrollo de Software IV, UCR Sede del Atlántico - Recinto Paraíso.

Backend: Spring Boot 3 (Java 21) + JWT + SQL Server.
Frontend: HTML5, CSS3 y JavaScript (Fetch API).

## Estructura
/backend -> Proyecto Spring Boot (API REST)
/frontend -> Cliente web (HTML, CSS, JS)


## Para ejecutar

1. Backend:
cd backend
mvn spring-boot:run


2. Frontend: clic derecho sobre `login.html` y abrir con **Live Server**.

3. Una vez logueado, desde el menú superior entra a **Paginación** para ver la consola del Laboratorio 9 (`dashboard_paginado.html`), con paginación (Primera, Anterior, Siguiente, Última) y el botón para ejecutar el Stored Procedure por estado.

## Usuarios

admin contraseña: Password123!
conductor1 contraseña: Password123!
operador1 contraseña: Password123!

## Laboratorio 9 - Endpoints nuevos

- `GET /api/v1/envios?page=&size=&sortBy=&direction=&busqueda=&estado=` — listado paginado
- `GET /api/v1/envios/procedimiento/{estado}` — ejecuta el Stored Procedure SP_OBTENER_ENVIOS_POR_ESTADO
