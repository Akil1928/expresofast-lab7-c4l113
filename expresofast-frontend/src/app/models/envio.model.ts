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