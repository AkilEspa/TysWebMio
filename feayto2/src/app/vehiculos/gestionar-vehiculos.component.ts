import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { VehiculoDto } from '../model/VehiculoDto';
import { VehiculoService } from '../services/vehiculo.service';

@Component({
  selector: 'app-gestionar-vehiculos',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './gestionar-vehiculos.component.html',
  styleUrl: './gestionar-vehiculos.component.css',
})
export class GestionarVehiculosComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly vehiculoService = inject(VehiculoService);

  municipio = '';
  cantidad = 1;
  vehiculos: VehiculoDto[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadVehiculos();
  }

  asignarVehiculos(): void {
    if (this.isLoading) {
      return;
    }
    if (!this.municipio.trim() || !Number.isInteger(this.cantidad) ||
        this.cantidad < 1 || this.cantidad > 100) {
      this.errorMessage = 'Indica un municipio y una cantidad entre 1 y 100.';
      this.successMessage = '';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.vehiculoService.asignarVehiculos({
      municipio: this.municipio.trim(),
      cantidad: this.cantidad,
    }).subscribe({
      next: () => {
        this.successMessage = 'Vehículos asignados correctamente.';
        this.loadVehiculos();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudieron asignar los vehículos.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadVehiculos(): void {
    this.isLoading = true;
    this.vehiculoService.getVehiculos().subscribe({
      next: (vehiculos) => {
        this.vehiculos = vehiculos;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudieron cargar los vehículos.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private readError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string') {
      return error.error;
    }
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
