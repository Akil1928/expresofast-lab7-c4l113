import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EnvioAvanzadoForm } from './envio-avanzado-form';

describe('EnvioAvanzadoForm', () => {
  let component: EnvioAvanzadoForm;
  let fixture: ComponentFixture<EnvioAvanzadoForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvioAvanzadoForm],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvioAvanzadoForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
