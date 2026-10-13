import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { VehiculoService } from '../services/vehiculo.service';
import { VehiculoDto } from '../model/VehiculoDto';

@Component({
  selector: 'app-crear-vehiculos',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './crear-vehiculos.component.html',
  styleUrl: './crear-vehiculos.component.css',
})
export class CrearVehiculosComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);

  numberOfVehicles = 5;
  vehiculos: VehiculoDto[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(private readonly service: VehiculoService) {}

  ngOnInit(): void {
    this.getVehiculos();
  }

  createVehicles(): void {
    if (this.isLoading || !Number.isInteger(this.numberOfVehicles) ||
        this.numberOfVehicles < 1 || this.numberOfVehicles > 5) {
      this.errorMessage = 'Elige una cantidad de vehículos entre 1 y 5.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.createVehicles(this.numberOfVehicles).subscribe({
      next: () => {
        this.successMessage = 'Vehículos creados correctamente.';
        this.getVehiculos();
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudieron crear los vehículos.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  getVehiculos(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.service.getVehiculos().subscribe({
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
