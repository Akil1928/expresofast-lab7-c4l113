// ===========================================================
// ExpresoFast - Laboratorio 9
// Consumo asíncrono de la API paginada (/api/v1/envios)
// Reutiliza getToken(), fetchWithAuth(), cerrarSesion(), API_BASE_URL
// y getUsername()/getRoles() definidos en app.js
// ===========================================================

const cuerpoTablaEnvios = document.getElementById('cuerpoTablaEnvios');

if (cuerpoTablaEnvios) {

    if (!getToken()) {
        window.location.href = 'login.html';
    }

    // API_BASE_URL en app.js = 'http://localhost:8080/api' (sin /v1)
    const API_V1_URL = API_BASE_URL.replace(/\/api$/, '/api/v1');

    const formFiltros = document.getElementById('formFiltros');
    const busquedaInput = document.getElementById('busqueda');
    const estadoFiltroSelect = document.getElementById('estadoFiltro');
    const tamanoPaginaSelect = document.getElementById('tamanoPagina');
    const ordenarPorSelect = document.getElementById('ordenarPor');
    const direccionOrdenSelect = document.getElementById('direccionOrden');
    const btnStoredProcedure = document.getElementById('btnStoredProcedure');
    const mensajeFiltros = document.getElementById('mensajeFiltros');

    const indicadorPagina = document.getElementById('indicadorPagina');
    const btnPrimera = document.getElementById('btnPrimera');
    const btnAnterior = document.getElementById('btnAnterior');
    const btnSiguiente = document.getElementById('btnSiguiente');
    const btnUltima = document.getElementById('btnUltima');

    const usuarioActual = document.getElementById('usuarioActual');
    const btnLogout = document.getElementById('btnLogout');

    if (usuarioActual) {
        usuarioActual.textContent = `👤 ${getUsername()} (${getRoles().join(', ')})`;
    }
    if (btnLogout) btnLogout.addEventListener('click', cerrarSesion);

    // currentPage es base 0 (lo que espera la API); se muestra +1 al usuario
    let currentPage = 0;
    let modoStoredProcedure = false;
    let ultimaRespuestaPagina = null;

    function formatearColones(valor) {
        const numero = Number(valor) || 0;
        return '₡' + numero.toLocaleString('es-CR', { minimumFractionDigits: 2 });
    }

    function formatearFecha(fechaIso) {
        if (!fechaIso) return '—';
        return new Date(fechaIso).toLocaleString('es-CR');
    }

    function renderizarFilas(envios) {
        if (!envios || envios.length === 0) {
            cuerpoTablaEnvios.innerHTML = '<tr><td colspan="5" style="padding: 1rem;">No hay envíos para este filtro.</td></tr>';
            return;
        }

        cuerpoTablaEnvios.innerHTML = envios.map(envio => `
            <tr style="border-bottom: 1px solid var(--border-color);">
                <td style="padding: 0.75rem;">${envio.codigoRastreo}</td>
                <td style="padding: 0.75rem;">${envio.direccionDestino}</td>
                <td style="padding: 0.75rem;">${formatearColones(envio.montoFlete)}</td>
                <td style="padding: 0.75rem;">
                    <span class="pill-status pill-${envio.estado}">${envio.estado}</span>
                </td>
                <td style="padding: 0.75rem;">${formatearFecha(envio.fechaCreacion)}</td>
            </tr>
        `).join('');
    }

    // -----------------------------------------------------------
    // Modo Paginado: GET /api/v1/envios
    // -----------------------------------------------------------
    async function cargarPaginado() {
        modoStoredProcedure = false;
        mensajeFiltros.textContent = '';
        cuerpoTablaEnvios.innerHTML = '<tr><td colspan="5" style="padding: 1rem;">Cargando envíos...</td></tr>';

        const size = tamanoPaginaSelect.value;
        const sortBy = ordenarPorSelect.value;
        const direction = direccionOrdenSelect.value;
        const busqueda = busquedaInput.value.trim();
        const estado = estadoFiltroSelect.value;

        const parametros = new URLSearchParams({
            page: currentPage,
            size,
            sortBy,
            direction
        });
        if (busqueda) parametros.append('busqueda', busqueda);
        if (estado) parametros.append('estado', estado);

        try {
            const respuesta = await fetchWithAuth(`${API_V1_URL}/envios?${parametros.toString()}`);
            if (!respuesta) return;

            if (!respuesta.ok) {
                throw new Error('Error al consultar los envíos: ' + respuesta.status);
            }

            const datos = await respuesta.json();
            ultimaRespuestaPagina = datos;
            renderizarFilas(datos.content);
            actualizarPaginador(datos);
        } catch (error) {
            cuerpoTablaEnvios.innerHTML = `<tr><td colspan="5" style="padding: 1rem;">No se pudo conectar con el servidor: ${error.message}</td></tr>`;
            console.error(error);
        }
    }

    // -----------------------------------------------------------
    // Modo Stored Procedure: GET /api/v1/envios/procedimiento/{estado}
    // No es paginado en backend, así que se deshabilita la barra de navegación.
    // -----------------------------------------------------------
    async function cargarViaStoredProcedure() {
        const estado = estadoFiltroSelect.value;

        if (!estado) {
            mensajeFiltros.textContent = 'Seleccione un estado específico para invocar el Stored Procedure.';
            mensajeFiltros.className = 'mensaje-form error';
            return;
        }

        modoStoredProcedure = true;
        mensajeFiltros.textContent = '';
        cuerpoTablaEnvios.innerHTML = '<tr><td colspan="5" style="padding: 1rem;">Ejecutando Stored Procedure...</td></tr>';

        try {
            const respuesta = await fetchWithAuth(`${API_V1_URL}/envios/procedimiento/${estado}`);
            if (!respuesta) return;

            if (!respuesta.ok) {
                throw new Error('Error al invocar el Stored Procedure: ' + respuesta.status);
            }

            const datos = await respuesta.json();
            renderizarFilas(datos);

            indicadorPagina.textContent = `Resultado del Stored Procedure (Total: ${datos.length} envíos)`;
            btnPrimera.disabled = true;
            btnAnterior.disabled = true;
            btnSiguiente.disabled = true;
            btnUltima.disabled = true;

            mensajeFiltros.textContent = `Mostrando resultados directos de SP_OBTENER_ENVIOS_POR_ESTADO para estado ${estado}.`;
            mensajeFiltros.className = 'mensaje-form exito';
        } catch (error) {
            cuerpoTablaEnvios.innerHTML = `<tr><td colspan="5" style="padding: 1rem;">${error.message}</td></tr>`;
            console.error(error);
        }
    }

    // -----------------------------------------------------------
    // Actualiza texto e indicador de estado del paginador (Page<T> de Spring)
    // -----------------------------------------------------------
    function actualizarPaginador(datosPagina) {
        const paginaVisible = datosPagina.number + 1; // Error 1: se muestra base 1 al usuario
        const totalPaginas = Math.max(datosPagina.totalPages, 1);

        indicadorPagina.textContent =
            `Página ${paginaVisible} de ${totalPaginas} (Total: ${datosPagina.totalElements} envíos)`;

        btnPrimera.disabled = datosPagina.first;
        btnAnterior.disabled = datosPagina.first;
        btnSiguiente.disabled = datosPagina.last;
        btnUltima.disabled = datosPagina.last;
    }

    // -----------------------------------------------------------
    // Eventos de navegación
    // -----------------------------------------------------------
    btnPrimera.addEventListener('click', () => {
        currentPage = 0;
        cargarPaginado();
    });

    btnAnterior.addEventListener('click', () => {
        if (currentPage > 0) currentPage--;
        cargarPaginado();
    });

    btnSiguiente.addEventListener('click', () => {
        if (ultimaRespuestaPagina && !ultimaRespuestaPagina.last) currentPage++;
        cargarPaginado();
    });

    btnUltima.addEventListener('click', () => {
        if (ultimaRespuestaPagina) currentPage = ultimaRespuestaPagina.totalPages - 1;
        cargarPaginado();
    });

    formFiltros.addEventListener('submit', (evento) => {
        evento.preventDefault();
        currentPage = 0; // toda nueva búsqueda/orden reinicia a la primera página (base 0)
        cargarPaginado();
    });

    btnStoredProcedure.addEventListener('click', () => {
        cargarViaStoredProcedure();
    });

    // -----------------------------------------------------------
    // Inicialización
    // -----------------------------------------------------------
    cargarPaginado();
}