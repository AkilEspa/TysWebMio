import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export interface AvailableVehicle {
  id: number;
  matricula: string;
  bateria: number;
  municipio: string;
}

export interface Rental {
  id: number;
  vehicleId: number;
  matricula: string;
  horas: number;
  importeCentimos: number;
  horaInicio: string;
  horaFin: string;
}

export interface CreateRentalRequest {
  vehicleId: number;
  hours: number;
  paymentIntentId: string;
}

@Injectable({ providedIn: 'root' })
export class RentalsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.bealquileresUrl}/alquileres`;

  getAvailableVehicles() {
    return this.http.get<AvailableVehicle[]>(`${this.apiUrl}/disponibles`, {
      withCredentials: true,
    });
  }

  getMyRentals() {
    return this.http.get<Rental[]>(`${this.apiUrl}/mios`, { withCredentials: true });
  }

  createRental(request: CreateRentalRequest) {
    return this.http.post<Rental>(this.apiUrl, request, { withCredentials: true });
  }
}
