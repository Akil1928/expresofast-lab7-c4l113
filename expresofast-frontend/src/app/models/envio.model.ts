/**
 * Representa un envio tal como lo retorna la API en los endpoints
 * de listado, rastreo y actualizacion de estado (EnvioDTO del backend).
 * Nota: el backend real usa "costo" como equivalente al "montoFlete"
 * solicitado en el enunciado original del laboratorio.
 */
export interface Envio {
  id: number;
  codigoRastreo: string;
  direccionDestino: string;
  montoFlete: number;
  estado: string; // PENDIENTE, EN_TRANSITO, ENTREGADO, CANCELADO
  fechaCreacion: string; // ISO string (LocalDateTime serializado)
}

/**
 * Representa la respuesta completa que retorna el backend al crear
 * un envio (EnvioResponseDTO), incluyendo datos de vehiculo y conductor.
 */
export interface EnvioCreado {
  id: number;
  codigoRastreo: string;
  direccionDestino: string;
  pesoKg: number;
  costo: number;
  estadoEnvio: string;
  placaVehiculo: string | null;
  nombreConductor: string | null;
}

/**
 * Payload requerido por el backend para registrar un nuevo envio
 * (EnvioRequestDTO). El backend valida que el peso no supere la
 * capacidad del vehiculo indicado.
 */
export interface CrearEnvioPayload {
  codigoRastreo: string;
  direccionDestino: string;
  pesoKg: number;
  costo: number;
  vehiculoId: number;
  conductorId: number;
}

export const ESTADOS_ENVIO = ['PENDIENTE', 'EN_TRANSITO', 'ENTREGADO', 'CANCELADO'] as const;
export type EstadoEnvio = typeof ESTADOS_ENVIO[number];
/**
 * Representa un paquete individual dentro del registro avanzado de un envio (Lab 11).
 */
export interface Paquete {
  descripcion: string;
  pesoKg: number;
}

/**
 * Payload para el registro avanzado de un envio con sus paquetes (Lab 11).
 * El campo "numeroTracking" se mapea al mismo "codigoRastreo" del backend.
 */
export interface EnvioRegistroPayload {
  numeroTracking: string;
  direccionDestino: string;
  costo: number;
  fechaDespacho: string; // ISO string
  fechaEntregaEstimada: string; // ISO string
  vehiculoId: number;
  conductorId: number;
  paquetes: Paquete[];
}

export interface CheckTrackingResponse {
  existe: boolean;
}