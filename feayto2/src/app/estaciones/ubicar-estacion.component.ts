import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MunicipioService } from '../services/municipio.service';

@Component({
  selector: 'app-ubicar-estacion',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './ubicar-estacion.component.html',
  styleUrl: './ubicar-estacion.component.css',
})
export class UbicarEstacionComponent {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly municipioService = inject(MunicipioService);

  municipio = '';
  nombre = '';
  numero: number | null = null;
  capacidad: number | null = null;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  ubicarEstacion(): void {
    if (this.isSubmitting) {
      return;
    }
    if (!this.municipio.trim() || !this.nombre.trim() ||
        !Number.isInteger(this.numero) || (this.numero ?? 0) < 0 ||
        !Number.isInteger(this.capacidad) || (this.capacidad ?? 0) < 1) {
      this.errorMessage = 'Completa municipio, calle, número y una capacidad válida.';
      this.successMessage = '';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.municipioService.setEstacion({
      municipio: this.municipio.trim(),
      nombre: this.nombre.trim(),
      numero: this.numero!,
      capacidad: this.capacidad!,
    }).subscribe({
      next: () => {
        this.successMessage = 'Estación ubicada correctamente.';
        this.isSubmitting = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error);
        this.isSubmitting = false;
        this.cdr.markForCheck();
      },
    });
  }

  private readError(error: HttpErrorResponse): string {
    if (typeof error.error === 'string') {
      return error.error;
    }
    return error.error?.detail ?? error.error?.message
      ?? 'No se pudo ubicar la estación. Comprueba que el municipio y la dirección estén cargados.';
  }
}
