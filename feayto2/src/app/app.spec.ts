import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { vi } from 'vitest';
import { App, LoginComponent } from './app';
import { routes } from './app.routes';
import { Usuario, SessionUser } from './services/usuario';
import { VehiculoService } from './services/vehiculo.service';
import { SimuladorService } from './services/simulador.service';
import { MunicipioService } from './services/municipio.service';

describe('App authentication routes', () => {
  let session: BehaviorSubject<SessionUser | null>;
  let usuario: {
    autoLogin: ReturnType<typeof vi.fn>;
    session$: BehaviorSubject<SessionUser | null>;
    sessionLoading$: Observable<boolean>;
    confirmRegistration: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    verify2FACode: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    session = new BehaviorSubject<SessionUser | null>(null);
    usuario = {
      autoLogin: vi.fn(() => of(session.value)),
      session$: session,
      sessionLoading$: of(false),
      confirmRegistration: vi.fn(() => of('Cuenta confirmada')),
      login: vi.fn(() => of({ status: 'CODE_REQUIRED', token: null, activationUrl: null })),
      verify2FACode: vi.fn(() => of({ status: 'AUTHENTICATED', token: null, activationUrl: null })),
      register: vi.fn(() => of({})),
      logout: vi.fn(() => {
        session.next(null);
        return of(undefined);
      }),
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideRouter(routes),
        { provide: Usuario, useValue: usuario },
        {
          provide: VehiculoService,
          useValue: {
            getVehiculos: () => of([]),
            createVehicles: () => of(undefined),
            asignarVehiculos: () => of(undefined),
          },
        },
        {
          provide: SimuladorService,
          useValue: {
            getCities: () => of([]),
            getVehiculos: () => of([]),
            getRuta: () => of([]),
            simulacionAleatoria: () => of(undefined),
            generateBicycle: () => of({}),
          },
        },
        { provide: MunicipioService, useValue: { setEstacion: () => of(undefined) } },
      ],
    }).compileComponents();
  });

  async function navigateTo(path: string) {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl(path);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('renders login and registration as separate routes', async () => {
    let fixture = await navigateTo('/login');
    expect(fixture.nativeElement.querySelector('app-login input[type="email"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('a[routerlink="/registro"]')).toBeTruthy();

    fixture = await navigateTo('/registro');
    expect(fixture.nativeElement.querySelector('app-registro input#nombre')).toBeTruthy();
  });

  it('redirects unauthenticated users away from the private menu', async () => {
    const fixture = await navigateTo('/menu');

    expect(TestBed.inject(Router).url).toBe('/login');
    expect(fixture.nativeElement.querySelector('app-menu')).toBeFalsy();
  });

  it('shows the account name and logs out from the protected menu', async () => {
    session.next({ nombre: 'María' });
    usuario.autoLogin.mockImplementation(() => of(session.value));
    const fixture = await navigateTo('/menu');

    expect(fixture.nativeElement.querySelector('.welcome-message')?.textContent).toContain('Hola, María');
    expect(fixture.nativeElement.querySelector('.portal-options')).toBeTruthy();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.portal-home > .btn-submit')?.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(usuario.logout).toHaveBeenCalled();
    expect(TestBed.inject(Router).url).toBe('/login');
  });

  it('renders the required 2FA setup after login when none is configured', async () => {
    const setupUrl = 'otpauth://totp/TySWeb:usuario%40municipio.es?secret=ABC&issuer=TySWeb';
    usuario.login.mockReturnValue(of({
      status: 'SETUP_REQUIRED',
      token: null,
      activationUrl: setupUrl,
    }));
    const fixture = await navigateTo('/login');
    const login = fixture.debugElement.query(By.directive(LoginComponent)).componentInstance as LoginComponent;
    login.loginForm.setValue({ email: 'usuario@municipio.es', password: 'Clave123!' });
    login.onSubmit();
    fixture.detectChanges();

    expect(login.activationUrl).toBe(setupUrl);
    expect(fixture.nativeElement.querySelector('.two-factor-setup')).toBeTruthy();
  });
});
