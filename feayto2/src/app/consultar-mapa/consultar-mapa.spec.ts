import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConsultarMapa } from './consultar-mapa';

describe('ConsultarMapa', () => {
  let component: ConsultarMapa;
  let fixture: ComponentFixture<ConsultarMapa>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultarMapa],
    }).compileComponents();

    fixture = TestBed.createComponent(ConsultarMapa);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
