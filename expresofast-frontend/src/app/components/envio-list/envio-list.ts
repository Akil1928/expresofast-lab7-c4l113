import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnvioService } from '../../services/envio.service';
import { Envio, ESTADOS_ENVIO } from '../../models/envio.model';

@Component({
  selector: 'app-envio-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './envio-list.html',
  styleUrl: './envio-list.css'
})
export class EnvioListComponent implements OnInit {
  private envioService = inject(EnvioService);
  private cdr = inject(ChangeDetectorRef);

  envios: Envio[] = [];
  estadosDisponibles = ESTADOS_ENVIO;
  cargando = true;
  mensajeError = '';
  mensajeExito = '';

  ngOnInit(): void {
    this.cargarEnvios();
  }

  cargarEnvios(): void {
    this.cargando = true;
    this.mensajeError = '';

    this.envioService.obtenerEnvios().subscribe({
      next: (datos) => {
        this.envios = datos;
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error(error);
        this.mensajeError = 'No se pudo conectar con el servidor backend.';
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  cambiarEstado(envio: Envio, evento: Event): void {
    const nuevoEstado = (evento.target as HTMLSelectElement).value;
    if (nuevoEstado === envio.estado) return;

    this.envioService.actualizarEstado(envio.id, nuevoEstado).subscribe({
      next: (actualizado) => {
        envio.estado = actualizado.estado;
        this.mensajeExito = `Envio ${envio.codigoRastreo} actualizado a ${nuevoEstado}.`;
        this.cdr.markForCheck();
        setTimeout(() => {
          this.mensajeExito = '';
          this.cdr.markForCheck();
        }, 3000);
      },
      error: (error) => {
        console.error(error);
        alert('No se pudo actualizar el estado: ' + (error.error?.message || error.message));
      }
    });
  }

  claseInsignia(estado: string): string {
    return `insignia insignia-${estado}`;
  }
}