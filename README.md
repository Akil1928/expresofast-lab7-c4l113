ExpresoFast - Consola de Operación Logística

Proyecto del curso IF0009 - Desarrollo de Software IV, UCR Sede del Atlántico - Recinto Paraíso.

Servidor: Spring Boot 3 (Java 21) + JWT + SQL Server. Frontend clásico: HTML5, CSS3 y JavaScript (Fetch API) — Laboratorios 6 a 9. Frontend SPA: Angular Standalone — Laboratorios 10 y 11.

Estructura del repositorio
/backend                -> Proyecto Spring Boot (API REST, JWT, JPA, Stored Procedures)
/frontend                -> Cliente clásico HTML/CSS/JS (Labs 6-9)
/expresofast-frontend    -> Cliente SPA en Angular Standalone (Labs 10-11)

Nota: los enunciados de los Laboratorios 10 y 11 piden dos carpetas nuevas e independientes ( expresofast-backendy expresofast-frontend). Se optó por reutilizar el backend real desarrollado en los Laboratorios 6-9 (con SQL Server, JWT y entidades Vehículo/Conductor) en lugar de crear uno nuevo simplificado, para no duplicar trabajo y mantener la integridad de los datos reales del proyecto. Por eso la carpeta se mantiene como /backenden vez de /expresofast-backend.

Cómo ejecutar el Backend (API REST)
cd backend
mvn spring-boot:run

La API queda disponible en http://localhost:8080. La conexión a SQL Server, JPA y JWT están en backend/src/main/resources/application.properties.

Cómo ejecutar el Frontend clásico (Labs 6-9)
Abrir la carpeta frontenden VS Code.
Haga clic derecho sobre login.html→ Abrir con Live Server .
Iniciar sesión y navegar entre Tablero , Nuevo Envío y Paginación .
Cómo ejecutar el Frontend Angular SPA (Labs 10-11)
Con el backend corriendo (paso anterior), abre otra terminal:
   cd expresofast-frontend
   ng serve
Abrir http://localhost:4200en el navegador.
La aplicación redirige por defecto a /envios. Desde la barra de navegación superior se accede a las vistas:
Envíos ( /envios): tabla con insignias de color por estado y selector para actualizar el estado.
Nuevo Envío ( /nuevo-envio): formulario simple con [(ngModel)](Laboratorio 10).
Registro Avanzado ( /envio-avanzado): formulario reactivo tipado con Múltiples paquetes por envío (Lab 11).
Rastrear Guía ( /rastreo): búsqueda por código de rastreo con barra de progreso visual.

Importante: el backend debe estar corriendo localhost:8080para que Angular pueda consumir la API. Ambos servidores ( mvn spring-boot:runy ng serve) deben estar activos al mismo tiempo, en terminales separadas.

Usuarios de prueba (solo para el cliente clásico con JWT)
Usuario	Contraseña	Rol
administración	Contraseña123!	ROL_ADMIN
conductor1	Contraseña123!	ROL_CONDUCTOR
operador1	Contraseña123!	ROL_OPERADOR

El cliente Angular (Labs 10-11) no requiere iniciar sesión : las rutas /api/v1/envios/**se dejaron abiertas ( permitAll) en SecurityConfigpara simplificar la integración, ya que estos enunciados no contemplan autenticación en el cliente SPA.

Puntos finales relevantes por laboratorio
Lab 6-9 (cliente clásico, requiere JWT)
GET /api/envios/optimizados
POST /api/envios
PATCH /api/envios/{id}/estado
GET /api/envios/{id}/bitacora
GET /api/v1/envios?page=&size=&sortBy=&direction=&busqueda=&estado=(paginación)
GET /api/v1/envios/procedimiento/{estado}(Procedimiento almacenado)
Laboratorio 10 (cliente Angular, sin JWT)
GET /api/v1/envios/todos— lista completa
GET /api/v1/envios/rastreo/{codigo}— búsqueda por código de rastreo
POST /api/v1/envios— registrador envío simple (requiere vehiculoId/ conductorIdexistente)
PATCH /api/v1/envios/{id}/estado— actualizar estado (sin bitácora de auditoría)
Lab 11 (cliente Angular, sin JWT)
GET /api/v1/envios/check-tracking/{numeroTracking}— valida si un código de rastreo ya existe (validador asíncrono)
POST /api/v1/envios/avanzado— registra un envío junto con su lista de paquetes asociados, de forma transaccional
Modelo de datos - Lab 11

Se agregó la tabla PAQUETEScon relación 1:N hacia Envio(FK envio_id, ON DELETE CASCADE), y dos columnas nuevas en Envio: fecha_despachoy fecha_entrega_estimada(nullable, para no romper registros existentes de labs anteriores). El campo numeroTrackingdel enunciado del Lab 11 se mapea directamente al codigoRastreoya existente en la entidad Envio— es el mismo concepto (identificador único de rastreo), por lo que no se duplicó el campo.

Notas de depuración
CORS: @CrossOrigin(origins = "http://localhost:4200") en EnvioPaginadoController+ configuración global de CORS en SecurityConfig.
HttpClient no provisto: resuelto con provideHttpClient(withFetch())en app.config.ts.
ngModel no enlaza: resuelto importante FormsModuleen cada componente independiente que usa [(ngModel)](Lab 10).
Vista no se actualiza tras una respuesta HTTP (hallazgo propio, Lab 10): el proyecto Angular se generó en modo zoneless (sin zone.js). Esto causaba que los cambios de estado dentro de .subscribe()no dispararan la detección de cambios automáticamente. Se resolvió inyectando ChangeDetectorRefy llamando markForCheck()después de cada actualización de estado asíncrona.
La compilación JIT falló (hallazgo propio, Lab 11): tras varias ediciones seguidas a un mismo componente, el servidor de desarrollo (Vite, usado internamente por Angular CLI 22) en ocasiones no logra recompilar correctamente en modo AOT y cae a un modo JIT no disponible en el proyecto. Se resuelve manteniendo y reiniciando ng servepor completo (no basta con recargar el navegador).
Fundación Teórica (Laboratorio 11)
1. UX y Escalabilidad: FormArray vs. campos estáticos ocultos

Usar FormArrayen lugar de 10 campos de texto estáticos y ocultos en HTML ofrece ventajas tanto en experiencia de usuario como en mantenibilidad del código:

Experiencia de Usuario (UX):

Cantidad real, no artificial: con campos estáticos ocultos, el desarrollador debe decidir de antemano un límite arbitrario (por ejemplo, "máximo 10 paquetes"). Si el operador necesita registrar un envío con 11 paquetes, la interfaz simplemente no lo permite. Con FormArray, la cantidad de bloques es dinámica y crece o decrece exactamente según la necesidad real del operador, sin límites artificiales.
Interfaz más limpia: con campos ocultos, el DOM contiene 10 bloques de entradas aunque solo se usan 2, generando inconsistencias visuales y posible confusión para tecnologías asistivas si no se gestionan con cuidado.
Feedback inmediato por bloque: cada FormGroupdentro del FormArraymantiene su propio estado de validación ( valid, touched, errors), permitiendo mostrar mensajes de error específicos para "Paquete 2" sin afectar a los demás. Replicar esto con campos estáticos requeriría lógica manual repetida para cada uno de los 10 bloques.

Mantenibilidad del código:

DRY (Don't Repite Yourself): con FormArray, la estructura de un paquete ( descripcion, pesoKg) se define una sola vez en el método crearPaqueteGroup(), y se reutiliza para generar tantas instancias como haga falta. Con campos estáticos, habría que declarar manualmente descripcion1... descripcion10y pesoKg1... pesoKg10, además de 10 bloques de validación casi idénticos.
Serialización trivial: this.envioForm.getRawValue().paquetes devuelve directamente un arreglo de objetos tipados, listo para enviar al backend. Con campos estáticos, habría que ensamblar manualmente ese arreglo leyendo campo por campo y filtrando los vacíos.
Validación a nivel de colección: reglas como "debe existir al menos un paquete" son naturales con FormArray.length, mientras que con campos estáticos requeriría lógica adicional para determinar cuántos de los 10 campos están realmente "en uso".

En resumen: FormArraymodela directamente la relación 1:N real del dominio (un envío tiene muchos paquetes), mientras que los campos estáticos ocultos son una simulación artificial de esa cardinalidad, con un costo de mantenimiento proporcional al límite máximo elegido arbitrariamente.

2. Ciclo de Eventos: validador síncrono (fechas) vs. validador asíncrono (seguimiento)

Validador síncrono ( fechaEntregaValidator): Se ejecuta dentro del mismo ciclo de ejecución (call stack) en el que Angular evalúa el estado del formulario. Cuando el usuario modifica fechaDespachoo fechaEntregaEstimada, Angular invoca inmediatamente la función, que lee ambos valores (ya disponibles en memoria, sin ninguna espera) y retorna de forma inmediata { fechaEntregaInvalida: true }o null. No hay ningún punto en el que la ejecución ceda el control al Event Loop: todo ocurre en el mismo frame, por lo que el estado del FormGrouppasa directamente a VALIDo INVALID.

Validador asíncrono ( trackingUnicoValidator): Necesita consultar el backend vía HTTP, y el tiempo de esa respuesta de red es indeterminado . Si Angular resuelve esto de forma síncrona, podría bloquear el hilo principal de JavaScript hasta que llegue la respuesta, congelando toda la interfaz, inaceptable en un entorno de un solo hilo. Por eso Angular exige que un AsyncValidatorFnretorno un Observableo Promise:

El validador llama a http.get(...), manejado por las API web del navegador, fuera del hilo principal de JavaScript.
Mientras la petición está en curso, el FormControlqueda en estado PENDINGy el hilo principal queda libre para procesar otros eventos.
Cuando llega la respuesta, el navegador coloca la devolución de llamada correspondiente en la cola de tareas/microtareas; el Event Loop, al encontrar el hilo libre, lo extrae y lo ejecuta.
Ese callback emite el valor a través del Observable( map/ catchError), y Angular, suscrito a él, recién entonces actualiza el estado del control a VALIDo INVALID.

En síntesis: el validador síncrono resuelve su resultado en el mismo tick porque toda la información ya está disponible localmente; el asíncrono debe ceder el control y esperar a que una operación externa (E/S gestionada por el navegador) se complete y su callback sea procesado por el Event Loop — el mismo mecanismo que permite a JavaScript, siendo de un solo hilo, manejar operaciones de red sin bloquear la interfaz de usuario.