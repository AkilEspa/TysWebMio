import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import * as QRCode from 'qrcode';
import { Usuario } from '../services/usuario';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly usuario = inject(Usuario);
  private readonly router = inject(Router);

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  readonly codeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  twoFactorStep: 'none' | 'setup' | 'code' = 'none';
  activationUrl = '';
  qrCodeDataUrl = '';
  isSubmitting = false;
  errorMessage = '';
  infoMessage = '';

  onSubmit(): void {
    if (this.isSubmitting) return;
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.infoMessage = '';
    this.usuario.login(this.loginForm.getRawValue())
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: (response) => {
          if (response.status === 'AUTHENTICATED') {
            this.completeLogin();
          } else if (response.status === 'SETUP_REQUIRED') {
            this.activationUrl = response.activationUrl ?? '';
            this.twoFactorStep = 'setup';
            this.infoMessage = 'Configura el doble factor con tu aplicación autenticadora y escribe el código de seis dígitos.';
            if (this.activationUrl.startsWith('otpauth://totp/')) {
              this.renderQrCode(this.activationUrl);
            }
          } else {
            this.twoFactorStep = 'code';
            this.infoMessage = 'Introduce el código de seis dígitos de tu aplicación autenticadora.';
          }
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.errorText(error);
        },
      });
  }

  verifyCode(): void {
    if (this.isSubmitting) return;
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.usuario.verify2FACode({
      ...this.loginForm.getRawValue(),
      ...this.codeForm.getRawValue(),
    }).pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: (response) => {
          if (response.status === 'AUTHENTICATED') this.completeLogin();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.errorText(error);
        },
      });
  }

  backToLogin(): void {
    this.twoFactorStep = 'none';
    this.activationUrl = '';
    this.qrCodeDataUrl = '';
    this.codeForm.reset();
    this.errorMessage = '';
    this.infoMessage = '';
  }

  private completeLogin(): void {
    this.usuario.autoLogin().subscribe({
      next: (session) => session
        ? this.router.navigateByUrl('/menu')
        : this.errorMessage = 'No se pudo validar la sesión. Vuelve a iniciar sesión.',
      error: (error: HttpErrorResponse) => this.errorMessage = this.errorText(error),
    });
  }

  private errorText(error: HttpErrorResponse): string {
    return typeof error.error === 'string'
      ? error.error
      : error.error?.message ?? error.error?.detail
        ?? 'No se pudo iniciar sesión. Comprueba tus datos y que la cuenta esté confirmada.';
  }

  private async renderQrCode(url: string): Promise<void> {
    try {
      this.qrCodeDataUrl = await QRCode.toDataURL(url, { width: 220, margin: 1 });
    } catch {
      this.errorMessage = 'No se pudo generar el código QR. Puedes copiar la dirección de configuración.';
    }
  }
}
