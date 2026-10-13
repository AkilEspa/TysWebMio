import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface RegistroRequest {
    nombre: string;
    apellidos: string;
    email: string;
    municipio: string;
    latitud: number;
    longitud: number;
    password: string;
    confirmPassword: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    status: 'SETUP_REQUIRED' | 'CODE_REQUIRED' | 'AUTHENTICATED';
    token: string | null;
    activationUrl: string | null;
}

export interface Verify2FARequest extends LoginRequest {
    code: string;
}

@Injectable({providedIn: 'root'})
export class Usuario {
    private http = inject(HttpClient);
    private apiUrl = `${environment.beusuUrl}/users`;

    register(request: RegistroRequest) {
        return this.http.put(`${this.apiUrl}/registerMunicipio`, request);
    }

    login(request: LoginRequest) {
        return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request);
    }

    verify2FACode(request: Verify2FARequest) {
        return this.http.post<LoginResponse>(`${this.apiUrl}/verify2FACode`, request);
    }

    confirmRegistration(token: string) {
        return this.http.get(`${this.apiUrl}/confirmar`, {
            params: { token },
            responseType: 'text',
        });
    }
}
