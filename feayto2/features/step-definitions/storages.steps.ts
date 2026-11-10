import { Then } from "@cucumber/cucumber";
import { CustomWorld } from "../support/world";
import assert from "assert";

Then(
  'se ha creado un token de sesión',
  async function (this: CustomWorld) {
    const token = await this.page.evaluate(
      () => sessionStorage.getItem('sessionToken')
    );
    assert.ok(
      token,
      'No se ha almacenado sessionToken'
    );
  }
);