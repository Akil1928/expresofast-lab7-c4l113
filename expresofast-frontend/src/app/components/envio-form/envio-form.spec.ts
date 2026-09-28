import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EnvioService } from '../../services/envio.service';
import { CrearEnvioPayload } from '../../models/envio.model';

@Component({
  selector: 'app-envio-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './envio-form.component.html',
  styleUrl: './envio-form.component.css'
})
export class EnvioFormComponent {
  private envioService = inject(EnvioService);
  private router = inject(Router);

  payload: CrearEnvioPayload = {
    codigoRastreo: '',
    direccionDestino: '',
    pesoKg: 0,
    costo: 0,
    vehiculoId: 1,
    conductorId: 1
  };

  enviando = false;
  mensajeError = '';
  mensajeExito = '';

  generarCodigoRastreo(): void {
    const numero = Math.floor(1000 + Math.random() * 9000);
    this.payload.codigoRastreo = `EXP-2026-${numero}`;
  }

  registrar(formulario: any): void {
    if (formulario.invalid) {
      this.mensajeError = 'Complete todos los campos obligatorios.';
      return;
    }

    this.enviando = true;
    this.mensajeError = '';
    this.mensajeExito = '';

    this.envioService.crearEnvio(this.payload).subscribe({
      next: () => {
        this.mensajeExito = `Envío ${this.payload.codigoRastreo} registrado correctamente.`;
        this.enviando = false;
        setTimeout(() => this.router.navigate(['/envios']), 1500);
      },
      error: (error) => {
        console.error(error);
        this.mensajeError = error.error?.message || error.error?.error || 'No se pudo registrar el envío.';
        this.enviando = false;
      }
    });
  }
}