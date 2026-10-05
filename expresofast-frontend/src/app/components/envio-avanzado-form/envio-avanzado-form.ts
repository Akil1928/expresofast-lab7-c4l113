import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
  FormGroup,
  FormArray
} from '@angular/forms';
import { Router } from '@angular/router';
import { EnvioService } from '../../services/envio.service';
import { EnvioRegistroPayload } from '../../models/envio.model';
import { fechaEntregaValidator } from '../../validators/fecha-entrega.validator';
import { trackingUnicoValidator } from '../../validators/tracking-unico.validator';

@Component({
  selector: 'app-envio-avanzado-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './envio-avanzado-form.html',
  styleUrl: './envio-avanzado-form.css'
})
export class EnvioAvanzadoFormComponent {
  private fb = inject(NonNullableFormBuilder);
  private envioService = inject(EnvioService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  enviando = false;
  mensajeError = '';
  mensajeExito = '';

  envioForm: FormGroup = this.fb.group({
    numeroTracking: this.fb.control('', {
      validators: [Validators.required, Validators.pattern(/^EXP-\d{4}$/)],
      asyncValidators: [trackingUnicoValidator(this.envioService)]
    }),
    direccionDestino: this.fb.control('', { validators: [Validators.required] }),
    costo: this.fb.control<number>(0, { validators: [Validators.required, Validators.min(1)] }),
    fechaDespacho: this.fb.control('', { validators: [Validators.required] }),
    fechaEntregaEstimada: this.fb.control('', { validators: [Validators.required] }),
    vehiculoId: this.fb.control<number>(1, { validators: [Validators.required, Validators.min(1)] }),
    conductorId: this.fb.control<number>(1, { validators: [Validators.required, Validators.min(1)] }),
    paquetes: this.fb.array([this.crearPaqueteGroup()])
  }, { validators: fechaEntregaValidator() });

  private crearPaqueteGroup(): FormGroup {
    return this.fb.group({
      descripcion: this.fb.control('', { validators: [Validators.required] }),
      pesoKg: this.fb.control<number>(0, { validators: [Validators.required, Validators.min(0.01)] })
    });
  }

  get paquetesArray(): FormArray {
    return this.envioForm.get('paquetes') as FormArray;
  }

  agregarPaquete(): void {
    this.paquetesArray.push(this.crearPaqueteGroup());
  }

  eliminarPaquete(index: number): void {
    if (this.paquetesArray.length <= 1) {
      this.mensajeError = 'Debe haber al menos un paquete en el envío.';
      this.cdr.markForCheck();
      return;
    }
    this.paquetesArray.removeAt(index);
  }

  generarNumeroTracking(): void {
    const numero = Math.floor(1000 + Math.random() * 9000);
    this.envioForm.get('numeroTracking')?.setValue(`EXP-${numero}`);
  }

  registrar(): void {
    if (this.envioForm.invalid || this.envioForm.pending) {
      this.envioForm.markAllAsTouched();
      this.mensajeError = 'Revise los campos marcados en rojo.';
      this.cdr.markForCheck();
      return;
    }

    this.enviando = true;
    this.mensajeError = '';
    this.mensajeExito = '';
    this.cdr.markForCheck();

    const valores = this.envioForm.getRawValue();

    const payload: EnvioRegistroPayload = {
      numeroTracking: valores.numeroTracking,
      direccionDestino: valores.direccionDestino,
      costo: valores.costo,
      fechaDespacho: this.normalizarFechaISO(valores.fechaDespacho),
      fechaEntregaEstimada: this.normalizarFechaISO(valores.fechaEntregaEstimada),
      vehiculoId: valores.vehiculoId,
      conductorId: valores.conductorId,
      paquetes: valores.paquetes
    };

    this.envioService.registrarEnvioAvanzado(payload).subscribe({
      next: () => {
        this.mensajeExito = `Envío ${payload.numeroTracking} registrado correctamente con ${payload.paquetes.length} paquete(s).`;
        this.enviando = false;
        this.cdr.markForCheck();
        setTimeout(() => this.router.navigate(['/envios']), 1800);
      },
      error: (error) => {
        console.error(error);
        this.mensajeError = error.error?.message || error.error?.error || 'No se pudo registrar el envío.';
        this.enviando = false;
        this.cdr.markForCheck();
      }
    });
  }

  private normalizarFechaISO(valor: string): string {
    return valor.length === 16 ? `${valor}:00` : valor;
  }
}