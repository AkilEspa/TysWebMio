import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { take } from 'rxjs';
import { MunicipioService, Feature } from '../services/municipio.service';
import { Usuario } from '../services/usuario';

@Component({
  imports: [CommonModule],
  selector: 'app-ubicar-estaciones',
  styleUrl: './ubicar-estaciones.css',
  templateUrl: './ubicar-estaciones.html',
  standalone: true,
})
export class UbicarEstaciones implements OnInit {
  private readonly municipioService = inject(MunicipioService);
  private readonly usuario = inject(Usuario);
  private readonly cdr = inject(ChangeDetectorRef);

  features: Feature[] = [];
  isLoading = true;
  errorMessage = '';
  municipio = '';

  ngOnInit(): void {
    this.usuario.session$.pipe(take(1)).subscribe((session) => {
      if (!session?.municipio) {
        this.errorMessage = 'No se pudo determinar el municipio de la sesión.';
        this.isLoading = false;
        this.cdr.markForCheck();
        return;
      }

      this.municipio = session.municipio;
      this.municipioService.getEstaciones(this.municipio).subscribe({
        next: (features) => {
          this.features = features;
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.readError(error);
          this.isLoading = false;
          this.cdr.markForCheck();
        },
      });
    });
  }

  private readError(error: HttpErrorResponse): string {
    if (typeof error.error === 'string') {
      return error.error;
    }
    return error.error?.detail ?? error.error?.message
      ?? `No se pudieron cargar las estaciones de ${this.municipio}.`;
  }
}
