import { When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/world";
import { credenciales } from "../data/credenciales";


When(
    'escribe {string} en {string}',
    async function (this: CustomWorld, texto: string, nombreCaja: string) {
       await this.page
        .locator(`[formControlName="${nombreCaja}"]`)
        .fill(texto);
    }
)

When(
  'introduce credenciales válidas',
  async function (this: CustomWorld) {
    await this.page
      .locator('[formControlName="email"]')
      .clear()

    await this.page
      .locator('[formControlName="pwd"]')
      .clear()

    const email = credenciales[0].email;
    const pwd = credenciales[0].pwd;

    await this.page
      .locator('[formControlName="email"]')
      .fill(email);
    
    await this.page
      .locator('[formControlName="pwd"]')
      .fill(pwd);

    //await this.page.waitForTimeout(1000);
  }
);
