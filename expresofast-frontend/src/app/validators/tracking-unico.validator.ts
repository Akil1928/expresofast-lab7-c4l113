import { AbstractControl, AsyncValidatorFn, ValidationErrors } from '@angular/forms';
import { of } from 'rxjs';
import { catchError, debounceTime, map, switchMap, take } from 'rxjs/operators';
import { EnvioService } from '../services/envio.service';

/**
 * Validador ASINCRONO (Lab 11, seccion 3.4). A diferencia del validador
 * cruzado de fechas (sincrono, se resuelve en el mismo ciclo del Event Loop
 * comparando dos valores ya disponibles en memoria), este validador necesita
 * esperar una respuesta de red (HTTP), cuyo tiempo de resolucion es
 * indeterminado. Angular por eso exige que retorne un Observable o Promise:
 * mientras esa promesa/observable no se resuelve, el FormControl queda en
 * estado PENDING, y Angular reintenta la validacion cuando el Observable emite.
 */
export function trackingUnicoValidator(envioService: EnvioService): AsyncValidatorFn {
  return (control: AbstractControl) => {
    const valor = control.value;

    // Solo consulta al backend si el formato basico ya es valido;
    // evita peticiones innecesarias mientras el usuario aun esta escribiendo.
    if (!valor || !/^EXP-\d{4}$/.test(valor)) {
      return of(null);
    }

    return of(valor).pipe(
      debounceTime(400), // espera a que el usuario deje de escribir
      switchMap(codigo => envioService.verificarTracking(codigo)),
      map((respuesta): ValidationErrors | null =>
        respuesta.existe ? { trackingTomado: true } : null
      ),
      catchError(() => of(null)), // si la API falla, no bloquea el formulario
      take(1)
    );
  };
}