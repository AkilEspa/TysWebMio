import { When, Then } from '@cucumber/cucumber';
import assert from 'node:assert';
import { CustomWorld } from '../support/world';

When(
  'introduce un correo electrónico único de prueba',
  async function (this: CustomWorld) {
    await this.page
      .locator('#email')
      .fill('registro-cucumber@example.test');
  }
);

Then(
  'se muestra un mensaje de éxito',
  async function (this: CustomWorld) {
    const successMessage = this.page.locator(
      '.registro .success-message[role="status"]'
    );
    await successMessage.waitFor({ state: 'visible' });
    assert.match(await successMessage.innerText(), /¡Registro completado!/);
  }
);
