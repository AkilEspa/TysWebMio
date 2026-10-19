import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
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

export interface SessionUser {
    nombre: string;
}

interface SessionResponse {
    nombre: string;
}

export interface Verify2FARequest extends LoginRequest {
    code: string;
}

@Injectable({providedIn: 'root'})
export class Usuario {
    private http = inject(HttpClient);
    private apiUrl = `${environment.beusuUrl}/users`;
    private readonly sessionSubject = new BehaviorSubject<SessionUser | null>(null);
    private readonly sessionLoadingSubject = new BehaviorSubject(true);
    private sessionRequest: Observable<SessionUser | null> | null = null;

    readonly session$ = this.sessionSubject.asObservable();
    readonly sessionLoading$ = this.sessionLoadingSubject.asObservable();

    register(request: RegistroRequest) {
        return this.http.put(`${this.apiUrl}/registerMunicipio`, request);
    }

    login(request: LoginRequest) {
        return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request, { withCredentials: true })
            .pipe(tap((response) => {
                if (response.status === 'AUTHENTICATED') {
                    this.sessionRequest = null;
                }
            }));
    }

    verify2FACode(request: Verify2FARequest) {
        return this.http.post<LoginResponse>(`${this.apiUrl}/verify2FACode`, request, { withCredentials: true })
            .pipe(tap((response) => {
                if (response.status === 'AUTHENTICATED') {
                    this.sessionRequest = null;
                }
            }));
    }

    autoLogin(): Observable<SessionUser | null> {
        if (!this.sessionRequest) {
            this.sessionLoadingSubject.next(true);
            this.sessionRequest = this.http.get<SessionResponse>(`${this.apiUrl}/validateSession`, {
                withCredentials: true,
            }).pipe(
                map((response) => ({ nombre: response.nombre })),
                catchError((error: { status?: number }) => error.status === 401
                    ? of(null)
                    : throwError(() => error)),
                tap((session) => this.sessionSubject.next(session)),
                finalize(() => {
                    this.sessionLoadingSubject.next(false);
                    this.sessionRequest = null;
                }),
                shareReplay({ bufferSize: 1, refCount: false }),
            );
        }
        return this.sessionRequest;
    }

    logout() {
        return this.http.post<void>(`${this.apiUrl}/logout`, {}, { withCredentials: true })
            .pipe(tap(() => {
                this.sessionSubject.next(null);
                this.sessionRequest = null;
            }));
    }

    confirmRegistration(token: string) {
        return this.http.get(`${this.apiUrl}/confirmar`, {
            params: { token },
            responseType: 'text',
        });
    }
}
