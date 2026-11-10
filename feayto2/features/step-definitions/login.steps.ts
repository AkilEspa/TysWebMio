import { Then } from '@cucumber/cucumber';
import assert from 'node:assert';
import { CustomWorld } from '../support/world';

Then(
  'las credenciales son aceptadas para iniciar sesión',
  async function (this: CustomWorld) {
    await this.page.waitForFunction(() => {
      const heading = document.querySelector('.login h1')?.textContent?.trim();
      return location.pathname.endsWith('/menu')
        || heading === 'Activa el doble factor'
        || heading === 'Introduce el código 2FA'
        || Boolean(document.querySelector('.login [role="alert"]'));
    });

    if (this.page.url().endsWith('/menu')) {
      return;
    }

    const heading = await this.page.locator('.login h1').innerText();
    assert.ok(
      ['Activa el doble factor', 'Introduce el código 2FA'].includes(heading),
      `No se aceptaron las credenciales: ${heading}`
    );
  }
);

