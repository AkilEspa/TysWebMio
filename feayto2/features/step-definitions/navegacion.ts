import { Given, Then } from "@cucumber/cucumber";
import { CustomWorld } from "../support/world";
import assert from "node:assert";

Given(
  'que el usuario está en la página de inicio de sesión',
  async function (this: CustomWorld) {
    await this.page.goto('http://localhost:4200/login');
  }
);

Given(
  'que el usuario está en la página de registro',
  async function (this: CustomWorld) {
    await this.page.goto('http://localhost:4200/registro');
  }
);

Then(
  'se muestra el menú principal',
  async function (this: CustomWorld) {
    await this.page.waitForURL('**/menu');
    assert.ok(
      this.page.url().endsWith('/menu')
    );
  }
);