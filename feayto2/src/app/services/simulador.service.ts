import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VehiculoDto } from '../model/VehiculoDto';
import { PuntoRutaDto } from '../model/PuntoRutaDto';
import { MunicipioDto } from '../model/MunicipioDto';
import { GeneracionDto } from '../model/GeneracionDto';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SimuladorService {
  private readonly http = inject(HttpClient);
  private readonly urlSimulador = `${environment.simuladorUrl}/simulador`;

  getCities() {
    return this.http.get<MunicipioDto[]>(`${this.urlSimulador}/getCities`);
  }

  simulacionAleatoria(city: string) {
    return this.http.post<void>(`${this.urlSimulador}/simulacionAleatoria`, {}, {
      params: { city },
    });
  }

  generateBicycle(city: string) {
    return this.http.post<GeneracionDto>(`${this.urlSimulador}/generateBicycle`, {}, {
      params: { city },
    });
  }

  getVehiculos(city: string): Observable<VehiculoDto[]> {
    return this.http.get<VehiculoDto[]>(`${this.urlSimulador}/getVehiculos/${encodeURIComponent(city)}`);
  }

  getRuta(city: string, matricula: string): Observable<PuntoRutaDto[]> {
    return this.http.get<PuntoRutaDto[]>(
      `${this.urlSimulador}/getRuta/${encodeURIComponent(city)}`,
      { params: { matricula } },
    );
  }
}
