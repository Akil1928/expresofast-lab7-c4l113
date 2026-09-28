import { Routes } from '@angular/router';
import { EnvioListComponent } from './components/envio-list/envio-list';
import { EnvioFormComponent } from './components/envio-form/envio-form';
import { EnvioTrackingComponent } from './components/envio-tracking/envio-tracking';

export const routes: Routes = [
  { path: '', redirectTo: 'envios', pathMatch: 'full' },
  { path: 'envios', component: EnvioListComponent },
  { path: 'nuevo-envio', component: EnvioFormComponent },
  { path: 'rastreo', component: EnvioTrackingComponent },
  { path: '**', redirectTo: 'envios' }
];