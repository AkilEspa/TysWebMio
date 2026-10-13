import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { App, LoginComponent } from './app';
import { Usuario } from './services/usuario';
import { VehiculoService } from './services/vehiculo.service';
import { SimuladorService } from './services/simulador.service';
import { MunicipioService } from './services/municipio.service';
import { vi } from 'vitest';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        {
          provide: Usuario,
          useValue: {
            confirmRegistration: () => of('Cuenta confirmada'),
            login: () => of({ status: 'CODE_REQUIRED', token: null, activationUrl: null }),
            verify2FACode: () => of({ status: 'AUTHENTICATED', token: 'token' }),
            register: () => of({}),
          },
        },
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
        {
          provide: MunicipioService,
          useValue: { setEstacion: () => of(undefined) },
        },
      ],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the registration form', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-registro')).toBeTruthy();
  });

  it('should show the email and password fields on the login tab', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const loginTab = Array.from(compiled.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Iniciar sesión'));

    loginTab?.click();
    fixture.detectChanges();

    expect(compiled.querySelector('app-login input[type="email"]')).toBeTruthy();
    expect(compiled.querySelector('app-login input[type="password"]')).toBeTruthy();
  });

  it('should show the authenticator setup QR automatically after login when 2FA is not configured', async () => {
    const setupUrl = 'otpauth://totp/TySWeb:usuario%40municipio.es?secret=ABC&issuer=TySWeb';
    const usuario = TestBed.inject(Usuario);
    vi.spyOn(usuario, 'login').mockReturnValue(of({
      status: 'SETUP_REQUIRED',
      token: null,
      activationUrl: setupUrl,
    }));

    const fixture = TestBed.createComponent(App);
    fixture.componentInstance.setAuthMode('login');
    await fixture.whenStable();
    const login = fixture.debugElement.query(By.directive(LoginComponent)).componentInstance as LoginComponent;
    login.loginForm.setValue({ email: 'usuario@municipio.es', password: 'Clave123!' });
    login.onSubmit();
    fixture.detectChanges();

    expect(login.activationUrl).toBe(setupUrl);
    expect(fixture.nativeElement.querySelector('.two-factor-setup')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('button[aria-label="Configurar 2FA"]')).toBeFalsy();
  });

  it('should show registration confirmation after following the email link', async () => {
    window.history.replaceState({}, '', '/?confirmToken=test-token');
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.confirmation-message')?.textContent)
      .toContain('¡Registro completado!');
    expect(compiled.querySelector('app-login')).toBeTruthy();
    expect(window.location.search).toBe('');

    window.history.replaceState({}, '', '/');
  });

  it('should show the portal options after a successful login on the current app', async () => {
    window.history.replaceState({}, '', '/login');
    const fixture = TestBed.createComponent(App);
    fixture.componentInstance.setAuthMode('login');
    await fixture.whenStable();
    fixture.detectChanges();

    const login = fixture.debugElement.query(By.directive(LoginComponent)).componentInstance as LoginComponent;
    login.loginForm.setValue({ email: 'usuario@municipio.es', password: 'Clave123!' });
    login.onSubmit();
    login.twoFactorForm.setValue({ code: '123456' });
    login.verifyCode();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(Array.from(compiled.querySelectorAll('.portal-options button')).map((button) =>
      button.textContent?.trim(),
    )).toEqual([
      'Crear vehículos',
      'Ubicar estaciones',
      'Gestionar estación',
      'Gestionar vehículos',
      'Gestionar tráfico',
    ]);
    expect(compiled.querySelector('.portal-home')).toBeTruthy();
    expect(compiled.querySelector('app-login')).toBeFalsy();
    compiled.querySelector<HTMLButtonElement>('.portal-options button')?.click();
    fixture.detectChanges();
    expect(compiled.querySelector('app-crear-vehiculos')).toBeTruthy();

    const options = compiled.querySelectorAll<HTMLButtonElement>('.portal-options button');
    options[1].click();
    fixture.detectChanges();
    expect(compiled.querySelector('app-ubicar-estacion')).toBeTruthy();

    options[3].click();
    fixture.detectChanges();
    expect(compiled.querySelector('app-gestionar-vehiculos')).toBeTruthy();
    window.history.replaceState({}, '', '/');
  });
});
