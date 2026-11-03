import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { Usuario } from './services/usuario';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AsyncPipe, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private readonly usuario = inject(Usuario);
  private readonly router = inject(Router);

  readonly session$ = this.usuario.session$;
  readonly sessionLoading$ = this.usuario.sessionLoading$;
  isAuthRoute = true;
  isConfirming = false;
  confirmationMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.usuario.autoLogin().subscribe({
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.errorText(error, 'No se pudo comprobar la sesión.');
      },
    });
    this.updateRoute(this.router.url);
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => this.updateRoute(event.urlAfterRedirects));

    const url = new URL(window.location.href);
    const token = url.searchParams.get('confirmToken');
    if (token) {
      this.isConfirming = true;
      this.usuario.confirmRegistration(token)
        .pipe(finalize(() => this.isConfirming = false))
        .subscribe({
          next: () => {
            this.confirmationMessage = 'Correo confirmado. Ya puedes iniciar sesión.';
            url.searchParams.delete('confirmToken');
            window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
            this.router.navigateByUrl('/login');
          },
          error: (error: HttpErrorResponse) => {
            this.errorMessage = this.errorText(
              error,
              'No se pudo confirmar la cuenta. El enlace puede haber caducado o haberse utilizado.',
            );
          },
        });
    }
  }

  logout(): void {
    this.errorMessage = '';
    this.usuario.logout().subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.errorText(error, 'No se pudo cerrar la sesión.');
      },
    });
  }

  private updateRoute(url: string): void {
    const path = url.split('?')[0];
    this.isAuthRoute = path === '/login' || path === '/registro';
  }

  private errorText(error: HttpErrorResponse, fallback: string): string {
    return typeof error.error === 'string'
      ? error.error
      : error.error?.message ?? error.error?.detail ?? fallback;
  }
}
