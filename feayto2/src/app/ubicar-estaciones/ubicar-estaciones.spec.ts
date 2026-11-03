import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';
import { UbicarEstaciones } from './ubicar-estaciones';
import { MunicipioService } from '../services/municipio.service';
import { Usuario, SessionUser } from '../services/usuario';

describe('UbicarEstaciones', () => {
  let component: UbicarEstaciones;
  let fixture: ComponentFixture<UbicarEstaciones>;
  let session: BehaviorSubject<SessionUser | null>;
  let getEstaciones: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    session = new BehaviorSubject<SessionUser | null>({
      nombre: 'María',
      municipio: 'Ciudad Real',
    });
    getEstaciones = vi.fn(() => of([{
      id: 'station-1',
      nombre: 'Plaza Mayor',
      numero: '1',
      capacidad: 12,
      latitud: 38.986,
      longitud: -3.929,
    }]));

    await TestBed.configureTestingModule({
      imports: [UbicarEstaciones],
      providers: [
        { provide: Usuario, useValue: { session$: session } },
        { provide: MunicipioService, useValue: { getEstaciones } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UbicarEstaciones);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads and displays the stations for the signed-in municipality', () => {
    fixture.detectChanges();

    expect(getEstaciones).toHaveBeenCalledWith('Ciudad Real');
    expect(fixture.nativeElement.textContent).toContain('Estaciones de Ciudad Real');
    expect(fixture.nativeElement.textContent.replace(/\s+/g, ' ')).toContain('Plaza Mayor 1');
    expect(fixture.nativeElement.textContent).toContain('12 bicicletas');
  });
});
