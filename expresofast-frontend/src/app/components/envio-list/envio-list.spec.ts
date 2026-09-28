import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EnvioList } from './envio-list';

describe('EnvioList', () => {
  let component: EnvioList;
  let fixture: ComponentFixture<EnvioList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvioList],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvioList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
