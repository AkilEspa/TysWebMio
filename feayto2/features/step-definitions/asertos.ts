import { Then } from "@cucumber/cucumber";
import { CustomWorld } from "../support/world"

Then(
    'aparece el texto {string}',
    async function (this: CustomWorld, texto: string) {
      await this.page.getByText(texto).first().waitFor({ state: 'visible' });
    }
)