# Feus

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 18.2.4.

## Development server

Run `npm start` for a dev server on `http://localhost:4201/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## User account and rentals

The user portal uses `beusu` on `http://localhost:8080` for registration, sign-in, session validation, and Stripe PaymentIntents. Vehicle availability and rental history are provided by `bealquileres` on `http://localhost:8081/api`.

Set the Stripe publishable key in `src/environments/environment.ts`. Configure `STRIPE_SECRET_KEY` in the `beusu` process; never put the secret key in frontend code or commit it. The default rental rate is 5 EUR per hour and the maximum rental duration is 168 whole hours. These can be configured with `RENTAL_PRICE_PER_HOUR_CENTS` and `RENTAL_MAX_HOURS`.

The municipal portal's bicycle purchase price is 100 EUR per bicycle by default. Configure `PURCHASE_PRICE_PER_VEHICLE_CENTS` on the `beusu` process to change it; the amount is calculated and verified by the server.

The shared `beusu` registration endpoint currently requires a valid municipality and coordinates, as well as the user's name, email, and password. For local development, both backends allow frontend origins on ports 4200 and 4201 by default.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
