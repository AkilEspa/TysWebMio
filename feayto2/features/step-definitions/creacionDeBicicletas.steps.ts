import { Then } from '@cucumber/cucumber';
import { CustomWorld } from '../support/world';

Then(
  'aparece la pantalla de creación de bicicletas',
  async function (this: CustomWorld) {
    await this.page
      .getByRole('heading', {
        level : 1,
        name: 'Creación de vehículos'
      })
      .waitFor({
        state: 'visible'
      });
  }
);