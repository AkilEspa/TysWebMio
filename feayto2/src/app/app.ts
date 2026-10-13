import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import * as QRCode from 'qrcode';
import { Registro } from './registro/registro';
import { Usuario } from './services/usuario';
import { CrearVehiculosComponent } from './crear-vehiculos/crear-vehiculos.component';
import { MapaComponent } from './mapa/mapa.component';
import { UbicarEstacionComponent } from './estaciones/ubicar-estacion.component';
import { GestionarVehiculosComponent } from './vehiculos/gestionar-vehiculos.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="login">
      <div class="section-heading">
        @if (twoFactorStep === 'none') {
          <p class="eyebrow">Bienvenido de nuevo</p>
          <h1>Inicia sesión</h1>
          <p>Accede a tu espacio de gestión municipal.</p>
        } @else if (twoFactorStep === 'setup') {
          <p class="eyebrow">Protege tu cuenta</p>
          <h1>Activa el doble factor</h1>
          <p>Es obligatorio configurar una aplicación de autenticación antes de iniciar sesión.</p>
        } @else {
          <p class="eyebrow">Verificación adicional</p>
          <h1>Introduce el código 2FA</h1>
          <p>Usa el código de seis dígitos de tu aplicación de autenticación.</p>
        }
      </div>

      @if (twoFactorStep === 'none') {
        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" novalidate>
          <div class="form-group">
            <label for="login-email">Correo electrónico</label>
            <input
              id="login-email"
              type="email"
              formControlName="email"
              autocomplete="email"
              placeholder="nombre@ayuntamiento.es"
              [class.invalid]="loginForm.controls.email.invalid && loginForm.controls.email.touched"
              [attr.aria-invalid]="loginForm.controls.email.invalid && loginForm.controls.email.touched"
            >
            @if (loginForm.controls.email.invalid && loginForm.controls.email.touched) {
              <span class="error-msg">Introduce un correo electrónico válido.</span>
            }
          </div>

          <div class="form-group">
            <label for="login-password">Contraseña</label>
            <input
              id="login-password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
              placeholder="Escribe tu contraseña"
              [class.invalid]="loginForm.controls.password.invalid && loginForm.controls.password.touched"
              [attr.aria-invalid]="loginForm.controls.password.invalid && loginForm.controls.password.touched"
            >
            @if (loginForm.controls.password.invalid && loginForm.controls.password.touched) {
              <span class="error-msg">La contraseña es obligatoria.</span>
            }
          </div>

          <button type="submit" class="btn-submit" [disabled]="isSubmitting">
            @if (isSubmitting) {
              <span class="spinner" role="status" aria-label="Iniciando sesión"></span>
            } @else {
              <span>Iniciar sesión</span>
            }
          </button>
        </form>
      } @else if (twoFactorStep === 'setup') {
        @if (activationUrl) {
          <div class="two-factor-setup">
            <p>Escanea este código QR con Google Authenticator u otra aplicación compatible.</p>
            @if (qrCodeDataUrl) {
              <img class="two-factor-qr" [src]="qrCodeDataUrl" alt="Código QR para configurar el doble factor">
            }
            <p class="otp-auth-url">Si no puedes escanearlo, usa esta clave de configuración:</p>
            <code>{{ activationUrl }}</code>
            <form [formGroup]="twoFactorForm" (ngSubmit)="verifyCode()" novalidate>
              <div class="form-group">
                <label for="setup-code">Código de verificación</label>
                <input id="setup-code" type="text" inputmode="numeric" autocomplete="one-time-code"
                  formControlName="code" maxlength="6" placeholder="123456">
                @if (twoFactorForm.controls.code.invalid && twoFactorForm.controls.code.touched) {
                  <span class="error-msg">Introduce un código de seis dígitos.</span>
                }
              </div>
              <button type="submit" class="btn-submit" [disabled]="isSubmitting">Confirmar y activar</button>
            </form>
          </div>
        } @else {
          <p class="error-message" role="alert">
            No se recibió la configuración del autenticador. Vuelve a iniciar sesión para intentarlo de nuevo.
          </p>
        }
        <button type="button" class="btn-secondary" (click)="backToLogin()">Volver al inicio de sesión</button>
      } @else {
        <form [formGroup]="twoFactorForm" (ngSubmit)="verifyCode()" novalidate>
          <div class="form-group">
            <label for="login-code">Código de verificación</label>
            <input id="login-code" type="text" inputmode="numeric" autocomplete="one-time-code"
              formControlName="code" maxlength="6" placeholder="123456">
            @if (twoFactorForm.controls.code.invalid && twoFactorForm.controls.code.touched) {
              <span class="error-msg">Introduce un código de seis dígitos.</span>
            }
          </div>
          <button type="submit" class="btn-submit" [disabled]="isSubmitting">Verificar código</button>
        </form>
        <button type="button" class="btn-secondary" (click)="backToLogin()">Volver al inicio de sesión</button>
      }

      @if (successMessage) {
        <p class="success-message" role="status">{{ successMessage }}</p>
      }
      @if (errorMessage) {
        <p class="error-message" role="alert">{{ errorMessage }}</p>
      }
    </section>
  `,
  styleUrl: './app.css',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly usuarioService = inject(Usuario);

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  readonly twoFactorForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });
  twoFactorStep: 'none' | 'setup' | 'login' = 'none';
  activationUrl = '';
  qrCodeDataUrl = '';
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  @Output() authenticated = new EventEmitter<void>();

  onSubmit(): void {
    if (this.isSubmitting) {
      return;
    }

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.usuarioService.login(this.loginForm.getRawValue())
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: (response) => {
          if (response.status === 'AUTHENTICATED') {
            this.authenticated.emit();
            return;
          }
          this.twoFactorStep = response.status === 'SETUP_REQUIRED' ? 'setup' : 'login';
          if (response.status === 'SETUP_REQUIRED') {
            if (!response.activationUrl?.startsWith('otpauth://totp/')) {
              this.errorMessage = 'El servidor no devolvió una configuración válida para el autenticador.';
              return;
            }
            this.activationUrl = response.activationUrl;
            this.renderQrCode(response.activationUrl);
          }
          this.successMessage = response.status === 'SETUP_REQUIRED'
            ? 'Antes de continuar, activa el doble factor de autenticación.'
            : 'Han pasado 15 días desde la última verificación. Introduce un código de tu aplicación.';
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage = typeof err.error === 'string'
            ? err.error
            : err.error?.message
              ?? err.error?.detail
              ?? 'No se ha podido iniciar sesión. Comprueba tus credenciales y que la cuenta esté confirmada.';
        },
      });
  }

  private async renderQrCode(url: string): Promise<void> {
    try {
      this.qrCodeDataUrl = await QRCode.toDataURL(url, { width: 240, margin: 1 });
    } catch {
      this.errorMessage = 'No se pudo generar el código QR. Usa la clave de configuración mostrada.';
    }
  }

  verifyCode(): void {
    if (this.isSubmitting) {
      return;
    }
    if (this.twoFactorForm.invalid) {
      this.twoFactorForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.usuarioService.verify2FACode({
      ...this.loginForm.getRawValue(),
      ...this.twoFactorForm.getRawValue(),
    })
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: (response) => {
          if (response.status === 'AUTHENTICATED' && response.token) {
            this.authenticated.emit();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(err);
        },
      });
  }

  backToLogin(): void {
    this.twoFactorStep = 'none';
    this.activationUrl = '';
    this.qrCodeDataUrl = '';
    this.twoFactorForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  private getErrorMessage(err: HttpErrorResponse): string {
    return typeof err.error === 'string'
      ? err.error
      : err.error?.message
        ?? err.error?.detail
        ?? 'No se pudo completar la solicitud. Comprueba tus credenciales e inténtalo de nuevo.';
  }
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    Registro,
    LoginComponent,
    CrearVehiculosComponent,
    MapaComponent,
    UbicarEstacionComponent,
    GestionarVehiculosComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly usuarioService = inject(Usuario);

  registrationConfirmed = false;
  isConfirmingRegistration = false;
  confirmationErrorMessage = '';
  isAuthenticated = false;
  selectedOption = 0;
  authMode: 'login' | 'register' = 'register';

  ngOnInit(): void {
    const url = new URL(window.location.href);
    const token = url.searchParams.get('confirmToken');

    if (!token) {
      return;
    }

    this.authMode = 'login';
    this.isConfirmingRegistration = true;

    this.usuarioService.confirmRegistration(token)
      .pipe(finalize(() => this.isConfirmingRegistration = false))
      .subscribe({
        next: () => {
          this.registrationConfirmed = true;
          url.searchParams.delete('confirmToken');
          window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
        },
        error: (err: HttpErrorResponse) => {
          this.confirmationErrorMessage = typeof err.error === 'string'
            ? err.error
            : err.error?.message
              ?? err.error?.detail
              ?? 'No se pudo confirmar la cuenta. El enlace puede haber caducado o ya haber sido utilizado.';
        },
      });
  }

  setAuthMode(mode: 'login' | 'register'): void {
    this.authMode = mode;
  }

  closeSession(): void {
    this.isAuthenticated = false;
    this.selectedOption = 0;
    this.authMode = 'login';
  }

  selectOption(option: number): void {
    this.selectedOption = option;
  }
}
