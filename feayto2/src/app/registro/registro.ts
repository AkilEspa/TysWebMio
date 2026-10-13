import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
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
  styleUrl: './registro.css',
})
export class Registro {
  private readonly fb = inject(FormBuilder);
  private readonly usuarioService = inject(Usuario);

  registroForm: FormGroup;
  successMessage = '';
  errorMessage = '';
  isSubmitting = false;

  constructor() {
    this.registroForm = this.fb.group({
      nombre: ['', Validators.required],
      apellidos: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      municipio: ['', Validators.required],
      latitud: [0, [Validators.required, Validators.min(-90), Validators.max(90)]],
      longitud: [0, [Validators.required, Validators.min(-180), Validators.max(180)]],
      password: [
        '',
        [
          Validators.required,
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d\s]).{8,}$/),
        ],
      ],
      confirmPassword: ['', Validators.required],
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  get f() {
    return this.registroForm.controls;
  }

  onSubmit(): void {
    if (this.isSubmitting) {
      return;
    }

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }

    const formValue = {
      ...this.registroForm.value,
      latitud: Number(this.registroForm.value.latitud),
      longitud: Number(this.registroForm.value.longitud),
    };

    this.isSubmitting = true;
    this.errorMessage = '';

    this.usuarioService.register(formValue)
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: () => {
          this.successMessage =
            '¡Registro completado! Hemos enviado un enlace a tu correo para confirmar la cuenta.';
          this.errorMessage = '';
          this.registroForm.reset({ latitud: 0, longitud: 0 });
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage = typeof err.error === 'string'
            ? err.error
            : err.error?.message
              ?? err.error?.detail
              ?? 'No se pudo completar el registro. Comprueba el servicio y vuelve a intentarlo.';
          this.successMessage = '';
        },
      });
  }
}
