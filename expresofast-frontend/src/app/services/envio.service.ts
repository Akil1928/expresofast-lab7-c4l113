import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

import { Envio, EnvioCreado, CrearEnvioPayload, EnvioRegistroPayload, CheckTrackingResponse } from '../models/envio.model';

@Injectable({
  providedIn: 'root'
})
export class EnvioService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.API_URL}envios`;
    /**
   * GET /api/v1/envios/check-tracking/{numeroTracking}
   * Usado por el validador asincrono del formulario reactivo (Lab 11).
   */
  verificarTracking(numeroTracking: string): Observable<CheckTrackingResponse> {
    return this.http.get<CheckTrackingResponse>(`${this.baseUrl}/check-tracking/${numeroTracking}`);
  }

  /**
   * POST /api/v1/envios/avanzado
   * Registra un envio junto con todos sus paquetes asociados (Lab 11).
   */
  registrarEnvioAvanzado(payload: EnvioRegistroPayload): Observable<Envio> {
    return this.http.post<Envio>(`${this.baseUrl}/avanzado`, payload);
  }

  /**
   * GET /api/v1/envios/todos — listado completo sin paginar.
   */
  obtenerEnvios(): Observable<Envio[]> {
    return this.http.get<Envio[]>(`${this.baseUrl}/todos`);
  }
  

  /**
   * GET /api/v1/envios/rastreo/{codigo}
   */
  obtenerPorRastreo(codigo: string): Observable<Envio> {
    return this.http.get<Envio>(`${this.baseUrl}/rastreo/${codigo}`);
  }

  /**
   * POST /api/v1/envios — registra un nuevo envio.
   */
  crearEnvio(payload: CrearEnvioPayload): Observable<EnvioCreado> {
    return this.http.post<EnvioCreado>(this.baseUrl, payload);
  }

  /**
   * PATCH /api/v1/envios/{id}/estado
   */
  actualizarEstado(id: number, nuevoEstado: string): Observable<Envio> {
    return this.http.patch<Envio>(`${this.baseUrl}/${id}/estado`, { nuevoEstado });
  }
  
}