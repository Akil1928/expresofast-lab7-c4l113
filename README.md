\## Laboratorio 7 - Suite de Pruebas Unitarias e Integracion



Se implemento una suite de pruebas automatizadas con JUnit 5, Mockito y MockMvc, certificando un minimo de 85% de cobertura de instrucciones en la capa de servicios (`cr.ac.ucr.paraiso.ie.c4h741.expresofast.business`) mediante JaCoCo.



\### Pruebas incluidas



\- \*\*EnvioServiceTest\*\* (20 pruebas): creacion de envios, validacion de capacidad de vehiculo, transiciones de estado validas e invalidas, cancelacion de envios, generacion automatica de bitacora, calculo de tarifas parametrizado.

\- \*\*AuthServiceTest\*\* (2 pruebas): login exitoso y fallido, aislando el AuthenticationManager con Mockito.

\- \*\*EnvioControllerTest\*\* / \*\*AuthControllerTest\*\* (5 pruebas): pruebas de corte de controlador con `@WebMvcTest` y `MockMvc`, verificando codigos HTTP (200, 400, 401, 404) y estructura de las respuestas JSON.



\### Como ejecutar las pruebas



Correr solo las pruebas:

```bash

cd backend

mvnw.cmd clean test

```



Correr las pruebas y certificar el umbral de cobertura del 85% (falla el build si no se cumple):

```bash

cd backend

mvnw.cmd clean verify

```



\### Ver el reporte de cobertura



Despues de ejecutar `mvn clean verify` (o `mvn clean test` seguido de `mvn jacoco:report`), abrir en el navegador:

