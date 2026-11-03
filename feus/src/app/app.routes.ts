import { inject } from '@angular/core';
import { CanActivateFn, Routes, Router } from '@angular/router';
import { map } from 'rxjs';
import { LoginComponent } from './login/login';
import { RegistroComponent } from './registro/registro';
import { RentalsComponent } from './rentals/rentals';
import { Usuario } from './services/usuario';

const authGuard: CanActivateFn = () => {
  const usuario = inject(Usuario);
  const router = inject(Router);
  return usuario.autoLogin().pipe(
    map((session) => session ? true : router.createUrlTree(['/login'])),
  );
};

const guestGuard: CanActivateFn = () => {
  const usuario = inject(Usuario);
  const router = inject(Router);
  return usuario.autoLogin().pipe(
    map((session) => session ? router.createUrlTree(['/menu']) : true),
  );
};

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'registro', component: RegistroComponent, canActivate: [guestGuard] },
  { path: 'menu', component: RentalsComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
