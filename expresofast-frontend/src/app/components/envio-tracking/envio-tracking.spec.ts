import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EnvioTracking } from './envio-tracking';

describe('EnvioTracking', () => {
  let component: EnvioTracking;
  let fixture: ComponentFixture<EnvioTracking>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvioTracking],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvioTracking);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
