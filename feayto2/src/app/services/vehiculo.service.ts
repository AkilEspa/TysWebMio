import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { VehiculoDto } from '../model/VehiculoDto';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VehiculoService {
  private readonly http = inject(HttpClient);
  private readonly urlAlquileres = `${environment.bealquileresUrl}/vehiculos`;

  createVehicles(numberOfVehicles: number) {
    return this.http.post<void>(`${this.urlAlquileres}/createVehicles`, numberOfVehicles, { withCredentials: true });
  }

  getVehiculos() {
    return this.http.get<VehiculoDto[]>(`${this.urlAlquileres}/getVehiculos`, { withCredentials: true });
  }

  asignarVehiculos(request: AsignarVehiculosRequest) {
    return this.http.post<void>(`${this.urlAlquileres}/asignarVehiculos`, request, { withCredentials: true });
  }
}

export interface AsignarVehiculosRequest {
  municipio: string;
  cantidad: number;
}
