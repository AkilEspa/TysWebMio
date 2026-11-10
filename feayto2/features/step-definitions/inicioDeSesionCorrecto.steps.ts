import { Given } from "@cucumber/cucumber";
import { credenciales } from "../data/credenciales";
import { CustomWorld } from "../support/world";

Given(
  'el usuario está logueado',
async function inicioDeSesionCorrecto(world: CustomWorld) {
  const credencial = credenciales[0];

  await world.page.goto('http://localhost:4200/login');

  await world.page
    .locator('[formControlName="email"]')
    .fill(credencial.email);

  await world.page
    .locator('[formControlName="pwd"]')
    .fill(credencial.pwd);

  await world.page
    .getByRole('button', { name: 'Entrar' })
    .click();

  await world.page.waitForURL('**/menu');
}
);