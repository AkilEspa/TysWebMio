import { When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/world";


When(
  'pulsa el botón de iniciar sesión',
  async function (this: CustomWorld) {

    await this.page
      .locator('button[type="submit"]')
      .click();
  }
);

When(
  'pulsa el botón de registrar',
  async function (this: CustomWorld) {
    await this.page.locator('button[type="submit"]').click();
  }
);

When(
  'pulsa el botón {string}',
  async function (this: CustomWorld, textoBoton: string) {
    await this.page
      .getByRole('button', { name: textoBoton })
      .click();
  }
);