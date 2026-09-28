ExpresoFast - Consola de Operación Logística

Proyecto del curso IF0009 - Desarrollo de Software IV, UCR Sede del Atlántico - Recinto Paraíso.

Backend: Spring Boot 3 (Java 21) + JWT + SQL Server. Frontend clásico: HTML5, CSS3 y JavaScript (Fetch API) — Laboratorios 6 a 9. Frontend SPA: Angular Standalone — Laboratorio 10.

Estructura del repositorio
/backend                -> Proyecto Spring Boot (API REST, JWT, JPA, Stored Procedures)
/frontend                -> Cliente clásico HTML/CSS/JS (Labs 6-9)
/expresofast-frontend    -> Cliente SPA en Angular Standalone (Lab 10)

Nota: el enunciado del Laboratorio 10 pide dos carpetas nuevas e independientes (expresofast-backend y expresofast-frontend). Se optó por reutilizar el backend real desarrollado en los Laboratorios 6-9 (con SQL Server, JWT y entidades Vehículo/ Conductor) en lugar de crear uno nuevo simplificado, para no duplicar trabajo y mantener la integridad de los datos reales del proyecto. Por eso la carpeta se mantiene como /backend en vez de /expresofast-backend.

Cómo ejecutar el Backend (API REST)
cd backend
mvn spring-boot:run

La API queda disponible en http://localhost:8080. La conexión a SQL Server, JPA y JWT están en backend/src/main/resources/application.properties.

Cómo ejecutar el Frontend clásico (Labs 6-9)
Abrir la carpeta frontend en VS Code.
Clic derecho sobre login.html → Open with Live Server.
Iniciar sesión y navegar entre Tablero, Nuevo Envío y Paginación.
Cómo ejecutar el Frontend Angular SPA (Lab 10)
Con el backend corriendo (paso anterior), abrir otra terminal:
   cd expresofast-frontend
   ng serve
Abrir http://localhost:4200 en el navegador.
La aplicación redirige por defecto a /envios. Desde la barra de navegación superior se accede a las 3 vistas:
Envíos (/envios): tabla con insignias de color por estado y selector para actualizar el estado.
Nuevo Envío (/nuevo-envio): formulario de registro con [(ngModel)].
Rastrear Guía (/rastreo): búsqueda por código de rastreo con barra de progreso visual.

Importante: el backend debe estar corriendo en localhost:8080 para que Angular pueda consumir la API. Ambos servidores (mvn spring-boot:run y ng serve) deben estar activos al mismo tiempo, en terminales separadas.

Usuarios de prueba (solo para el cliente clásico con JWT)
Usuario	Contraseña	Rol
admin	Password123!	ROLE_ADMIN
conductor1	Password123!	ROLE_CONDUCTOR
operador1	Password123!	ROLE_OPERADOR

El cliente Angular (Lab 10) no requiere login: las rutas /api/v1/envios/** se dejaron abiertas (permitAll) en SecurityConfig para simplificar la integración, ya que el enunciado del Lab 10 no contempla autenticación en el cliente SPA.

Endpoints relevantes por laboratorio
Lab 6-9 (cliente clásico, requieren JWT)
GET /api/envios/optimizados
POST /api/envios
PATCH /api/envios/{id}/estado
GET /api/envios/{id}/bitacora
GET /api/v1/envios?page=&size=&sortBy=&direction=&busqueda=&estado= (paginación)
GET /api/v1/envios/procedimiento/{estado} (Stored Procedure)
Lab 10 (cliente Angular, sin JWT)
GET /api/v1/envios/todos — listado completo
GET /api/v1/envios/rastreo/{codigo} — búsqueda por código de rastreo
POST /api/v1/envios — registrar envío (requiere vehiculoId/conductorId existentes)
PATCH /api/v1/envios/{id}/estado — actualizar estado (sin bitácora de auditoría)
Notas de depuración
CORS: @CrossOrigin(origins = "http://localhost:4200") en EnvioPaginadoController + configuración global de CORS en SecurityConfig.
HttpClient no provisto: resuelto con provideHttpClient(withFetch()) en app.config.ts.
ngModel no enlaza: resuelto importando FormsModule en cada componente standalone que usa [(ngModel)].
Vista no se actualiza tras una respuesta HTTP (hallazgo propio): el proyecto Angular se generó en modo zoneless (sin zone.js). Esto causaba que los cambios de estado dentro de .subscribe() no dispararan la detección de cambios automáticamente. Se resolvió inyectando ChangeDetectorRef y llamando markForCheck() después de cada actualización de estado asíncrona, en los 3 componentes.