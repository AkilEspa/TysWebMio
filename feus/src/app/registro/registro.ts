import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { Usuario } from '../services/usuario';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './registro.html',
  styleUrl: './registro.scss',
})
export class RegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly usuario = inject(Usuario);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    apellidos: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    municipio: ['', Validators.required],
    latitud: [0, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitud: [0, [Validators.required, Validators.min(-180), Validators.max(180)]],
    password: ['', [
      Validators.required,
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/),
    ]],
    confirmPassword: ['', Validators.required],
  }, { validators: this.passwordMatchValidator });

  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    return !password || !confirmPassword || password === confirmPassword
      ? null
      : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.isSubmitting) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.usuario.register(value)
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: () => {
          this.successMessage = 'Cuenta creada. Te hemos enviado un enlace para confirmar tu correo.';
          this.form.reset({ latitud: 0, longitud: 0 });
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = typeof error.error === 'string'
            ? error.error
            : error.error?.message ?? error.error?.detail
              ?? 'No se pudo completar el registro. Comprueba tus datos e inténtalo de nuevo.';
        },
      });
  }
}
