import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface EstacionRequest {
  municipio: string;
  nombre: string;
  numero: number;
  capacidad: number;
}

@Injectable({ providedIn: 'root' })
export class MunicipioService {
  private readonly http = inject(HttpClient);
  private readonly urlMunicipios = `${environment.bealquileresUrl}/municipios`;

  setEstacion(request: EstacionRequest) {
    return this.http.post<void>(`${this.urlMunicipios}/setEstacion`, request, { withCredentials: true });
  }
}
