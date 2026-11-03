import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface EstacionRequest {
  municipio: string;
  nombre: string;
  numero: number;
  capacidad: number;
}

export interface EstacionMapaRequest {
  latitud: number;
  longitud: number;
  capacidad: number;
}

export interface Feature {
  id: string;
  nombre: string;
  numero: string | null;
  capacidad: number;
  latitud: number | null;
  longitud: number | null;
}

@Injectable({ providedIn: 'root' })
export class MunicipioService {
  private readonly http = inject(HttpClient);
  private readonly urlMunicipios = `${environment.bealquileresUrl}/municipios`;

  setEstacion(request: EstacionRequest) {
    return this.http.post<void>(`${this.urlMunicipios}/setEstacion`, request, { withCredentials: true });
  }

  setEstacionMapa(request: EstacionMapaRequest) {
    return this.http.post<Feature>(
      `${this.urlMunicipios}/setEstacionMapa`,
      request,
      { withCredentials: true },
    );
  }

  getEstaciones(municipio: string) {
    return this.http.get<Feature[]>(
      `${this.urlMunicipios}/getEstaciones/${encodeURIComponent(municipio)}`,
      { withCredentials: true },
    );
  }
}
