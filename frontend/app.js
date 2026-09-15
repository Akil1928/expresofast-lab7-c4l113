// ===========================================================
// ExpresoFast - Consumo asincrono de la API Spring Boot con JWT
// ===========================================================

const API_BASE_URL = 'http://localhost:8080/api';

// -----------------------------------------------------------
// Utilidades de sesion (localStorage)
// -----------------------------------------------------------
function getToken() {
    return localStorage.getItem('jwt_token');
}

function getUsername() {
    return localStorage.getItem('jwt_username');
}

function getRoles() {
    const roles = localStorage.getItem('jwt_roles');
    return roles ? JSON.parse(roles) : [];
}

function tieneRol(...rolesPermitidos) {
    const rolesUsuario = getRoles();
    return rolesPermitidos.some(r => rolesUsuario.includes(r));
}

function guardarSesion(token, username, roles) {
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('jwt_username', username);
    localStorage.setItem('jwt_roles', JSON.stringify(roles));
}

function cerrarSesion() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_username');
    localStorage.removeItem('jwt_roles');
    window.location.href = 'login.html';
}

// -----------------------------------------------------------
// fetchWithAuth: agrega el header Authorization automaticamente
// y maneja expiracion de sesion (401/403)
// -----------------------------------------------------------
async function fetchWithAuth(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`,
        ...(options.headers || {})
    };

    const respuesta = await fetch(url, { ...options, headers });

    if (respuesta.status === 401 || respuesta.status === 403) {
        cerrarSesion();
        throw new Error('Sesion expirada. Por favor inicie sesion nuevamente.');
    }

    return respuesta;
}

// ===========================================================
// LOGICA DE login.html
// ===========================================================
const formLogin = document.getElementById('formLogin');

if (formLogin) {
    const mensajeLogin = document.getElementById('mensajeLogin');

    formLogin.addEventListener('submit', async (evento) => {
        evento.preventDefault();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        try {
            const respuesta = await fetch(`${API_BASE_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const datos = await respuesta.json();

            if (!respuesta.ok) {
                throw new Error(datos.error || 'Usuario o contraseña incorrectos.');
            }

            guardarSesion(datos.token, datos.username, datos.roles);
            window.location.href = 'index.html';
        } catch (error) {
            mensajeLogin.textContent = error.message;
            mensajeLogin.className = 'mensaje-form error';
        }
    });
}

// ===========================================================
// LOGICA DE index.html
// ===========================================================
const enviosGrid = document.getElementById('enviosGrid');

if (enviosGrid) {

    // Si no hay token, redirigir de inmediato al login
    if (!getToken()) {
        window.location.href = 'login.html';
    }

    let enviosCache = [];
    let bitacoraCache = [];
    let filtroActual = 'TODOS';

    const filtrosLista = document.getElementById('filtrosLista');
    const formEnvio = document.getElementById('formEnvio');
    const mensajeForm = document.getElementById('mensajeForm');
    const usuarioActual = document.getElementById('usuarioActual');
    const btnLogout = document.getElementById('btnLogout');
    const seccionNuevoEnvio = document.getElementById('nuevo-envio');

    const modalBitacora = document.getElementById('modalBitacora');
    const modalBitacoraTitulo = document.getElementById('modalBitacoraTitulo');
    const bitacoraLista = document.getElementById('bitacoraLista');
    const btnCerrarModal = document.getElementById('btnCerrarModal');
    const filtroFechaInicio = document.getElementById('filtroFechaInicio');
    const filtroFechaFin = document.getElementById('filtroFechaFin');
    const btnLimpiarFiltroFecha = document.getElementById('btnLimpiarFiltroFecha');

    // -----------------------------------------------------------
    // Inicializar interfaz segun el usuario y su rol
    // -----------------------------------------------------------
    function inicializarInterfazSegunRol() {
        usuarioActual.textContent = `${getUsername()} (${getRoles().join(', ')})`;

        // ROLE_CONDUCTOR: ocultar formulario de creacion de envios
        if (tieneRol('ROLE_CONDUCTOR') && !tieneRol('ROLE_ADMIN', 'ROLE_OPERADOR')) {
            seccionNuevoEnvio.style.display = 'none';
        }
    }

    btnLogout.addEventListener('click', cerrarSesion);

    // -----------------------------------------------------------
    // Cargar envios desde el backend (GET /api/envios/optimizados)
    // -----------------------------------------------------------
    async function cargarEnvios() {
        try {
            enviosGrid.innerHTML = '<p class="cargando">Cargando envios...</p>';
            const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios/optimizados`);

            if (!respuesta.ok) {
                throw new Error('Error al consultar los envios: ' + respuesta.status);
            }

            enviosCache = await respuesta.json();
            renderizarEnvios();
        } catch (error) {
            enviosGrid.innerHTML = `<p class="cargando">No se pudo conectar con el servidor: ${error.message}</p>`;
            console.error(error);
        }
    }

    // -----------------------------------------------------------
    // Renderizar tarjetas segun el filtro activo y el rol
    // -----------------------------------------------------------
    function renderizarEnvios() {
        const enviosFiltrados = filtroActual === 'TODOS'
            ? enviosCache
            : enviosCache.filter(e => e.estadoEnvio === filtroActual);

        if (enviosFiltrados.length === 0) {
            enviosGrid.innerHTML = '<p class="cargando">No hay envios para este filtro.</p>';
            return;
        }

        const mostrarBotonBitacora = tieneRol('ROLE_ADMIN', 'ROLE_OPERADOR');

        enviosGrid.innerHTML = enviosFiltrados.map(envio => `
            <article class="envio-card" data-id="${envio.id}">
                <h3>${envio.codigoRastreo}</h3>
                <span class="pill-status pill-${envio.estadoEnvio}">${envio.estadoEnvio}</span>
                <p><strong>Destino:</strong> ${envio.direccionDestino}</p>
                <p><strong>Peso:</strong> ${envio.pesoKg} kg</p>
                <p><strong>Costo:</strong> ₡${envio.costo}</p>
                <p><strong>Vehiculo:</strong> ${envio.placaVehiculo || 'N/A'}</p>
                <p><strong>Conductor:</strong> ${envio.nombreConductor || 'N/A'}</p>
                <div class="envio-acciones">
                    <button class="btn-transito" onclick="cambiarEstado(${envio.id}, 'EN_TRANSITO')">Marcar en Transito</button>
                    <button class="btn-entregado" onclick="cambiarEstado(${envio.id}, 'ENTREGADO')">Marcar Entregado</button>
                    ${mostrarBotonBitacora ? `<button class="btn-bitacora" onclick="abrirBitacora(${envio.id}, '${envio.codigoRastreo}')">Ver Bitacora</button>` : ''}
                </div>
            </article>
        `).join('');
    }

    // -----------------------------------------------------------
    // Registrar un nuevo envio (POST /api/envios)
    // -----------------------------------------------------------
    if (formEnvio) {
        formEnvio.addEventListener('submit', async (evento) => {
            evento.preventDefault();

            const payload = {
                codigoRastreo: document.getElementById('codigoRastreo').value,
                direccionDestino: document.getElementById('direccionDestino').value,
                pesoKg: parseFloat(document.getElementById('pesoKg').value),
                costo: parseFloat(document.getElementById('costo').value),
                vehiculoId: parseInt(document.getElementById('vehiculoId').value),
                conductorId: parseInt(document.getElementById('conductorId').value)
            };

            try {
                const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });

                const datos = await respuesta.json();

                if (!respuesta.ok) {
                    throw new Error(datos.error || 'No se pudo registrar el envio.');
                }

                mostrarMensajeForm('Envio registrado correctamente.', 'exito');
                formEnvio.reset();
                cargarEnvios();
            } catch (error) {
                mostrarMensajeForm(error.message, 'error');
            }
        });
    }

    function mostrarMensajeForm(texto, tipo) {
        mensajeForm.textContent = texto;
        mensajeForm.className = `mensaje-form ${tipo}`;
    }

    // -----------------------------------------------------------
    // Cambiar estado de un envio (PATCH /api/envios/{id}/estado)
    // -----------------------------------------------------------
    window.cambiarEstado = async function (envioId, nuevoEstado) {
        const observaciones = prompt('Observaciones para este cambio de estado (opcional):', '') || '';

        try {
            const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios/${envioId}/estado`, {
                method: 'PATCH',
                body: JSON.stringify({ nuevoEstado, observaciones })
            });

            const datos = await respuesta.json();

            if (!respuesta.ok) {
                throw new Error(datos.error || 'No se pudo actualizar el estado.');
            }

            cargarEnvios();
        } catch (error) {
            alert('Error: ' + error.message);
            console.error(error);
        }
    };

    // -----------------------------------------------------------
    // Modal de Bitacora de Auditoria
    // -----------------------------------------------------------
    window.abrirBitacora = async function (envioId, codigoRastreo) {
        modalBitacoraTitulo.textContent = `Bitacora del Envio ${codigoRastreo}`;
        bitacoraLista.innerHTML = '<p class="cargando">Cargando bitacora...</p>';
        filtroFechaInicio.value = '';
        filtroFechaFin.value = '';
        modalBitacora.classList.remove('oculto');

        try {
            const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios/${envioId}/bitacora`);

            if (!respuesta.ok) {
                throw new Error('No se pudo cargar la bitacora.');
            }

            bitacoraCache = await respuesta.json();
            renderizarBitacora();
        } catch (error) {
            bitacoraLista.innerHTML = `<p class="cargando">${error.message}</p>`;
        }
    };

    function renderizarBitacora() {
        let entradas = bitacoraCache;

        const desde = filtroFechaInicio.value ? new Date(filtroFechaInicio.value) : null;
        const hasta = filtroFechaFin.value ? new Date(filtroFechaFin.value) : null;

        if (desde) {
            entradas = entradas.filter(b => new Date(b.fechaCambio) >= desde);
        }
        if (hasta) {
            // Incluir todo el dia "hasta"
            const hastaFin = new Date(hasta);
            hastaFin.setHours(23, 59, 59, 999);
            entradas = entradas.filter(b => new Date(b.fechaCambio) <= hastaFin);
        }

        if (entradas.length === 0) {
            bitacoraLista.innerHTML = '<p class="cargando">No hay registros en este rango de fechas.</p>';
            return;
        }

        bitacoraLista.innerHTML = entradas.map(b => `
            <div class="bitacora-item">
                <div class="bitacora-transicion">
                    <span class="pill-status pill-${b.estadoAnterior}">${b.estadoAnterior}</span>
                    <span class="bitacora-flecha">&rarr;</span>
                    <span class="pill-status pill-${b.estadoNuevo}">${b.estadoNuevo}</span>
                </div>
                <p><strong>Fecha:</strong> ${new Date(b.fechaCambio).toLocaleString('es-CR')}</p>
                <p><strong>Usuario:</strong> ${b.usuario}</p>
                ${b.observaciones ? `<p><strong>Observaciones:</strong> ${b.observaciones}</p>` : ''}
            </div>
        `).join('');
    }

    filtroFechaInicio.addEventListener('change', renderizarBitacora);
    filtroFechaFin.addEventListener('change', renderizarBitacora);

    btnLimpiarFiltroFecha.addEventListener('click', () => {
        filtroFechaInicio.value = '';
        filtroFechaFin.value = '';
        renderizarBitacora();
    });

    btnCerrarModal.addEventListener('click', () => modalBitacora.classList.add('oculto'));
    modalBitacora.addEventListener('click', (evento) => {
        if (evento.target === modalBitacora) modalBitacora.classList.add('oculto');
    });

    // -----------------------------------------------------------
    // Filtros de estado interactivos
    // -----------------------------------------------------------
    filtrosLista.addEventListener('click', (evento) => {
        const boton = evento.target.closest('.filtro-btn');
        if (!boton) return;

        document.querySelectorAll('.filtro-btn').forEach(b => b.classList.remove('activo'));
        boton.classList.add('activo');

        filtroActual = boton.dataset.estado;
        renderizarEnvios();
    });

    // -----------------------------------------------------------
    // Inicializacion
    // -----------------------------------------------------------
    document.addEventListener('DOMContentLoaded', () => {
        inicializarInterfazSegunRol();
        cargarEnvios();
    });
}