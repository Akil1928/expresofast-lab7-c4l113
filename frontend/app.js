// ===========================================================
// ExpresoFast - Consumo asíncrono de la API Spring Boot con JWT
// ===========================================================

const API_BASE_URL = 'http://localhost:8080/api';

// -----------------------------------------------------------
// Utilidades de sesión (localStorage)
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
// fetchWithAuth: agrega el header Authorization automáticamente
// y maneja expiración de sesión (401/403)
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
        throw new Error('Sesión expirada o sin permisos. Por favor inicie sesión nuevamente.');
    }

    return respuesta;
}

// ===========================================================
// LÓGICA DE login.html
// ===========================================================
const formLogin = document.getElementById('formLogin');

if (formLogin) {
    const mensajeLogin = document.getElementById('mensajeLogin');

    formLogin.addEventListener('submit', async (evento) => {
        evento.preventDefault();

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value.trim();

        try {
            const respuesta = await fetch(`${API_BASE_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const datos = await respuesta.json();

            if (!respuesta.ok) {
                throw new Error(datos.error || datos.message || 'Usuario o contraseña incorrectos.');
            }

            guardarSesion(datos.token || datos.accessToken, datos.username || username, datos.roles);
            window.location.href = 'index.html';
        } catch (error) {
            mensajeLogin.textContent = error.message;
            mensajeLogin.className = 'mensaje-form error';
        }
    });
}

// ===========================================================
// LÓGICA DE index.html
// ===========================================================
const enviosGrid = document.getElementById('enviosGrid');

if (enviosGrid) {

    // Redirigir al login si no hay token
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
    // Inicializar interfaz según el usuario y su rol
    // -----------------------------------------------------------
    function inicializarInterfazSegunRol() {
        if (usuarioActual) {
            usuarioActual.textContent = `👤 ${getUsername()} (${getRoles().join(', ')})`;
        }

        // CONDUCTOR puro: ocultar formulario de creación
        if (tieneRol('ROLE_CONDUCTOR') && !tieneRol('ROLE_ADMIN', 'ROLE_OPERADOR')) {
            if (seccionNuevoEnvio) seccionNuevoEnvio.style.display = 'none';
        }
    }

    if (btnLogout) btnLogout.addEventListener('click', cerrarSesion);

    // -----------------------------------------------------------
    // Cargar envíos desde el backend (GET /api/envios/optimizados)
    // -----------------------------------------------------------
    async function cargarEnvios() {
        try {
            enviosGrid.innerHTML = '<p class="cargando">Cargando envíos...</p>';
            const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios/optimizados`);

            if (!respuesta) return;

            if (!respuesta.ok) {
                throw new Error('Error al consultar los envíos: ' + respuesta.status);
            }

            enviosCache = await respuesta.json();
            renderizarEnvios();
        } catch (error) {
            enviosGrid.innerHTML = `<p class="cargando">No se pudo conectar con el servidor: ${error.message}</p>`;
            console.error(error);
        }
    }

    // -----------------------------------------------------------
    // Renderizar tarjetas según el filtro activo y el rol (RBAC)
    // -----------------------------------------------------------
    function renderizarEnvios() {
        const enviosFiltrados = filtroActual === 'TODOS'
            ? enviosCache
            : enviosCache.filter(e => (e.estadoEnvio || e.estado) === filtroActual);

        if (enviosFiltrados.length === 0) {
            enviosGrid.innerHTML = '<p class="cargando">No hay envíos para este filtro.</p>';
            return;
        }

        const mostrarBotonBitacora = tieneRol('ROLE_ADMIN', 'ROLE_OPERADOR');
        const puedeIniciarTransito = tieneRol('ROLE_ADMIN', 'ROLE_OPERADOR');
        const puedeEntregar = tieneRol('ROLE_ADMIN', 'ROLE_CONDUCTOR');

        enviosGrid.innerHTML = enviosFiltrados.map(envio => {
            const estado = envio.estadoEnvio || envio.estado;

            // Determinar dinámicamente el botón de cambio de estado
            let botonAccionHTML = '';
            if (estado === 'PENDIENTE' && puedeIniciarTransito) {
                botonAccionHTML = `<button class="btn-transito btn-primario" onclick="cambiarEstado(${envio.id}, 'EN_TRANSITO')">Marcar en Tránsito</button>`;
            } else if (estado === 'EN_TRANSITO' && puedeEntregar) {
                botonAccionHTML = `<button class="btn-entregado btn-primario" onclick="cambiarEstado(${envio.id}, 'ENTREGADO')">Marcar Entregado</button>`;
            }

            return `
                <article class="envio-card" data-id="${envio.id}">
                    <h3>${envio.codigoRastreo}</h3>
                    <span class="pill-status pill-${estado}">${estado}</span>
                    <p><strong>Destino:</strong> ${envio.direccionDestino}</p>
                    <p><strong>Peso:</strong> ${envio.pesoKg} kg</p>
                    <p><strong>Costo:</strong> ₡${envio.costo}</p>
                    <p><strong>Vehículo:</strong> ${envio.placaVehiculo || envio.vehiculoId || 'N/A'}</p>
                    <p><strong>Conductor:</strong> ${envio.nombreConductor || envio.conductorId || 'N/A'}</p>
                    <div class="envio-acciones" style="display: flex; gap: 0.5rem; margin-top: 0.75rem; flex-wrap: wrap;">
                        ${botonAccionHTML}
                        ${mostrarBotonBitacora ? `<button class="btn-bitacora btn-secundario" onclick="abrirBitacora(${envio.id}, '${envio.codigoRastreo}')">Ver Bitácora</button>` : ''}
                    </div>
                </article>
            `;
        }).join('');
    }

    // -----------------------------------------------------------
    // Registrar un nuevo envío (POST /api/envios)
    // -----------------------------------------------------------
    if (formEnvio) {
        formEnvio.addEventListener('submit', async (evento) => {
            evento.preventDefault();

            const payload = {
                codigoRastreo: document.getElementById('codigoRastreo').value.trim(),
                direccionDestino: document.getElementById('direccionDestino').value.trim(),
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

                if (!respuesta) return;

                const datos = await respuesta.json();

                if (!respuesta.ok) {
                    throw new Error(datos.error || datos.message || 'No se pudo registrar el envío.');
                }

                mostrarMensajeForm('Envío registrado correctamente.', 'exito');
                formEnvio.reset();
                cargarEnvios();
            } catch (error) {
                mostrarMensajeForm(error.message, 'error');
            }
        });
    }

    function mostrarMensajeForm(texto, tipo) {
        if (mensajeForm) {
            mensajeForm.textContent = texto;
            mensajeForm.className = `mensaje-form ${tipo}`;
        }
    }

    // -----------------------------------------------------------
    // Cambiar estado de un envío (PATCH /api/envios/{id}/estado)
    // -----------------------------------------------------------
    window.cambiarEstado = async function (envioId, nuevoEstado) {
        const observaciones = prompt('Observaciones para este cambio de estado (opcional):', '') || '';

        try {
            const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios/${envioId}/estado`, {
                method: 'PATCH',
                body: JSON.stringify({ nuevoEstado, estado: nuevoEstado, observaciones })
            });

            if (!respuesta) return;

            if (!respuesta.ok) {
                const datos = await respuesta.json();
                throw new Error(datos.error || datos.message || 'No se pudo actualizar el estado.');
            }

            cargarEnvios();
        } catch (error) {
            alert('Error: ' + error.message);
            console.error(error);
        }
    };

    // -----------------------------------------------------------
    // Modal de Bitácora de Auditoría
    // -----------------------------------------------------------
    window.abrirBitacora = async function (envioId, codigoRastreo) {
        modalBitacoraTitulo.textContent = `Bitácora del Envío ${codigoRastreo}`;
        bitacoraLista.innerHTML = '<p class="cargando">Cargando bitácora...</p>';
        filtroFechaInicio.value = '';
        filtroFechaFin.value = '';
        modalBitacora.classList.remove('oculto');

        try {
            const respuesta = await fetchWithAuth(`${API_BASE_URL}/envios/${envioId}/bitacora`);

            if (!respuesta) return;

            if (!respuesta.ok) {
                throw new Error('No se pudo cargar la bitácora.');
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
            entradas = entradas.filter(b => new Date(b.fechaCambio || b.fecha) >= desde);
        }
        if (hasta) {
            const hastaFin = new Date(hasta);
            hastaFin.setHours(23, 59, 59, 999);
            entradas = entradas.filter(b => new Date(b.fechaCambio || b.fecha) <= hastaFin);
        }

        if (entradas.length === 0) {
            bitacoraLista.innerHTML = '<p class="cargando">No hay registros en este rango de fechas.</p>';
            return;
        }

        bitacoraLista.innerHTML = entradas.map(b => `
            <div class="bitacora-item" style="padding: 0.75rem; border-bottom: 1px solid #e2e8f0;">
                <div class="bitacora-transicion">
                    <span class="pill-status pill-${b.estadoAnterior}">${b.estadoAnterior || 'INICIAL'}</span>
                    <span class="bitacora-flecha">&rarr;</span>
                    <span class="pill-status pill-${b.estadoNuevo}">${b.estadoNuevo || b.estado}</span>
                </div>
                <p><strong>Fecha:</strong> ${new Date(b.fechaCambio || b.fecha || Date.now()).toLocaleString('es-CR')}</p>
                <p><strong>Usuario:</strong> ${b.usuarioCambio || b.usuario || 'Sistema'}</p>
                ${b.observaciones ? `<p><strong>Observaciones:</strong> ${b.observaciones}</p>` : ''}
            </div>
        `).join('');
    }

    if (filtroFechaInicio) filtroFechaInicio.addEventListener('change', renderizarBitacora);
    if (filtroFechaFin) filtroFechaFin.addEventListener('change', renderizarBitacora);

    if (btnLimpiarFiltroFecha) {
        btnLimpiarFiltroFecha.addEventListener('click', () => {
            filtroFechaInicio.value = '';
            filtroFechaFin.value = '';
            renderizarBitacora();
        });
    }

    if (btnCerrarModal) btnCerrarModal.addEventListener('click', () => modalBitacora.classList.add('oculto'));
    if (modalBitacora) {
        modalBitacora.addEventListener('click', (evento) => {
            if (evento.target === modalBitacora) modalBitacora.classList.add('oculto');
        });
    }

    // -----------------------------------------------------------
    // Filtros de estado interactivos
    // -----------------------------------------------------------
    if (filtrosLista) {
        filtrosLista.addEventListener('click', (evento) => {
            const boton = evento.target.closest('.filtro-btn');
            if (!boton) return;

            document.querySelectorAll('.filtro-btn').forEach(b => b.classList.remove('activo'));
            boton.classList.add('activo');

            filtroActual = boton.dataset.estado;
            renderizarEnvios();
        });
    }

    // -----------------------------------------------------------
    // Inicialización Directa
    // -----------------------------------------------------------
    inicializarInterfazSegunRol();
    cargarEnvios();
}