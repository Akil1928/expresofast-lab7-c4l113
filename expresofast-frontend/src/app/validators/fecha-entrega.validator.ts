import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validador síncrono a nivel de FormGroup (Lab 11, Validación Cruzada).
 * Verifica que fechaEntregaEstimada sea estrictamente posterior a fechaDespacho.
 *
 * Se aplica al grupo completo (no a un control individual) porque necesita
 * leer el valor de DOS campos relacionados para tomar una decisión conjunta.
 * Angular ejecuta este tipo de validador de forma síncrona, en el mismo
 * ciclo de ejecución (sin pasar por el Event Loop/microtask queue), por
 * eso retorna directamente un objeto (o null) y no un Observable/Promise.
 */
export function fechaEntregaValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const fechaDespacho = control.get('fechaDespacho')?.value;
    const fechaEntregaEstimada = control.get('fechaEntregaEstimada')?.value;

    if (!fechaDespacho || !fechaEntregaEstimada) {
      return null; // Los validadores required individuales ya cubren el caso vacio.
    }

    const despacho = new Date(fechaDespacho);
    const entrega = new Date(fechaEntregaEstimada);

    if (entrega <= despacho) {
      return { fechaEntregaInvalida: true };
    }

    return null;
  };
}