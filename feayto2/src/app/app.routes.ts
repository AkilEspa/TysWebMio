import { inject } from '@angular/core';
import { CanActivateFn, Routes } from '@angular/router';
import { Router } from '@angular/router';
import { map } from 'rxjs';
import { LoginComponent, MenuComponent } from './app';
import { Registro } from './registro/registro';
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
  { path: 'registro', component: Registro, canActivate: [guestGuard] },
  { path: 'menu', component: MenuComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
