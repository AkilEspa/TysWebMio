import { Given } from '@cucumber/cucumber';
import { CustomWorld } from '../support/world';
import { inicioDeSesionCorrecto } from './inicioDeSesionCorrecto.steps';

Given(
  'que el usuario ha iniciado sesión correctamente',
  async function (this: CustomWorld) {
    await inicioDeSesionCorrecto(this);
  }
);