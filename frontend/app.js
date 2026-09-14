// ===========================================================
// ExpresoFast - Consumo asincrono de la API Spring Boot
// ===========================================================

const API_BASE_URL = 'http://localhost:8080/api/envios';

let enviosCache = [];
let filtroActual = 'TODOS';

const enviosGrid = document.getElementById('enviosGrid');
const filtrosLista = document.getElementById('filtrosLista');
const formEnvio = document.getElementById('formEnvio');
const mensajeForm = document.getElementById('mensajeForm');

// -----------------------------------------------------------
// Cargar envios desde el backend (GET /api/envios/optimizados)
// -----------------------------------------------------------
async function cargarEnvios() {
    try {
        enviosGrid.innerHTML = '<p class="cargando">Cargando envios...</p>';
        const respuesta = await fetch(`${API_BASE_URL}/optimizados`);

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
// Renderizar tarjetas segun el filtro activo
// -----------------------------------------------------------
function renderizarEnvios() {
    const enviosFiltrados = filtroActual === 'TODOS'
        ? enviosCache
        : enviosCache.filter(e => e.estadoEnvio === filtroActual);

    if (enviosFiltrados.length === 0) {
        enviosGrid.innerHTML = '<p class="cargando">No hay envios para este filtro.</p>';
        return;
    }

    enviosGrid.innerHTML = enviosFiltrados.map(envio => `
        <article class="envio-card" data-id="${envio.id}">
            <h3>${envio.codigoRastreo}</h3>
            <span class="pill-status pill-${envio.estadoEnvio}">${envio.estadoEnvio}</span>
            <p><strong>Destino:</strong> ${envio.direccionDestino}</p>
            <p><strong>Peso:</strong> ${envio.pesoKg} kg</p>
            <p><strong>Costo:</strong> ₡${envio.costo}</p>
            <p><strong>Vehiculo:</strong> ${envio.vehiculo ? envio.vehiculo.placa : 'N/A'}</p>
            <p><strong>Conductor:</strong> ${envio.conductor ? (envio.conductor.nombre + ' ' + envio.conductor.apellidos) : 'N/A'}</p>
            <div class="envio-acciones">
                <button class="btn-transito" onclick="cambiarEstado(${envio.id}, 'EN_TRANSITO')">Marcar en Transito</button>
                <button class="btn-entregado" onclick="cambiarEstado(${envio.id}, 'ENTREGADO')">Marcar Entregado</button>
            </div>
        </article>
    `).join('');
}

// -----------------------------------------------------------
// Registrar un nuevo envio (POST /api/envios)
// -----------------------------------------------------------
formEnvio.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const payload = {
        codigoRastreo: document.getElementById('codigoRastreo').value,
        direccionDestino: document.getElementById('direccionDestino').value,
        pesoKg: parseFloat(document.getElementById('pesoKg').value),
        costo: parseFloat(document.getElementById('costo').value),
        vehiculo: { id: parseInt(document.getElementById('vehiculoId').value) },
        conductor: { id: parseInt(document.getElementById('conductorId').value) }
    };

    try {
        const respuesta = await fetch(API_BASE_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
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

function mostrarMensajeForm(texto, tipo) {
    mensajeForm.textContent = texto;
    mensajeForm.className = `mensaje-form ${tipo}`;
}

// -----------------------------------------------------------
// Cambiar estado de un envio (PATCH /api/envios/{id}/estado)
// -----------------------------------------------------------
async function cambiarEstado(envioId, nuevoEstado) {
    try {
        const respuesta = await fetch(`${API_BASE_URL}/${envioId}/estado`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado: nuevoEstado })
        });

        if (!respuesta.ok) {
            const datos = await respuesta.json();
            throw new Error(datos.error || 'No se pudo actualizar el estado.');
        }

        cargarEnvios();
    } catch (error) {
        alert('Error: ' + error.message);
        console.error(error);
    }
}

// -----------------------------------------------------------
// Filtros interactivos
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
document.addEventListener('DOMContentLoaded', cargarEnvios);
