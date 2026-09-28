import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EnvioService } from '../../services/envio.service';
import { Envio } from '../../models/envio.model';

@Component({
  selector: 'app-envio-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './envio-tracking.html',
  styleUrl: './envio-tracking.css'
})
export class EnvioTrackingComponent {
  private envioService = inject(EnvioService);
  private cdr = inject(ChangeDetectorRef);

  codigoBusqueda = '';
  envio: Envio | null = null;
  buscando = false;
  mensajeError = '';
  buscoAlgunaVez = false;

  private secuenciaEstados = ['PENDIENTE', 'EN_TRANSITO', 'ENTREGADO'];

  buscar(): void {
    if (!this.codigoBusqueda.trim()) return;

    this.buscando = true;
    this.buscoAlgunaVez = true;
    this.mensajeError = '';
    this.envio = null;
    this.cdr.markForCheck();

    this.envioService.obtenerPorRastreo(this.codigoBusqueda.trim()).subscribe({
      next: (datos) => {
        this.envio = datos;
        this.buscando = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error(error);
        this.mensajeError = error.status === 404
          ? `No se encontró ningún envío con el código "${this.codigoBusqueda}".`
          : 'Error al consultar el envío.';
        this.buscando = false;
        this.cdr.markForCheck();
      }
    });
  }

  porcentajeProgreso(): number {
    if (!this.envio) return 0;
    if (this.envio.estado === 'CANCELADO') return 100;

    const indice = this.secuenciaEstados.indexOf(this.envio.estado);
    if (indice === -1) return 0;
    return ((indice + 1) / this.secuenciaEstados.length) * 100;
  }

  claseInsignia(estado: string): string {
    return `insignia insignia-${estado}`;
  }
}