import { AfterViewInit, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaymentService } from '../services/payments';

interface StripeCardChangeEvent {
  complete: boolean;
  error?: { message: string };
}

interface StripeCardElement {
  mount(element: HTMLElement): void;
  on(event: 'change', handler: (event: StripeCardChangeEvent) => void): void;
}

interface StripeClient {
  elements(): {
    create(type: 'card'): StripeCardElement;
  };
  confirmCardPayment(
    clientSecret: string,
    paymentData: { payment_method: { card: StripeCardElement } },
  ): Promise<{
    error?: { message: string };
    paymentIntent?: { status: string };
  }>;
}

declare const Stripe: (publishableKey: string) => StripeClient;

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './payments.css',
  templateUrl: './payments.html',
})
export class Payments implements AfterViewInit {
  @ViewChild('cardElement', { static: true })
  private cardElement!: ElementRef<HTMLDivElement>;

  private readonly paymentService = inject(PaymentService);
  private stripe?: StripeClient;
  private card?: StripeCardElement;

  quantity = 1;
  clientSecret = '';
  paymentIntentId = '';
  amountInCents = 0;
  unitPriceInCents = 0;
  cardComplete = false;
  isCreatingPayment = false;
  isPaying = false;
  errorMessage = '';
  successMessage = '';

  get unitPriceLabel(): string {
    return this.formatEuro(this.unitPriceInCents);
  }

  get totalPriceLabel(): string {
    return this.formatEuro(this.amountInCents);
  }

  ngAfterViewInit(): void {
    if (!environment.stripePublishableKey) {
      this.errorMessage = 'Falta configurar la clave pública de Stripe.';
    }
  }

  startPayment(): void {
    if (this.isCreatingPayment || this.isPaying || this.clientSecret) {
      return;
    }
    this.errorMessage = '';
    this.successMessage = '';
    this.cardComplete = false;

    if (!Number.isInteger(this.quantity) || this.quantity < 1 || this.quantity > 100) {
      this.errorMessage = 'Selecciona entre 1 y 100 bicicletas.';
      return;
    }

    this.isCreatingPayment = true;
    this.paymentIntentId = '';
    this.unitPriceInCents = 0;
    this.paymentService.createPurchaseIntent(this.quantity).subscribe({
      next: ({ clientSecret, paymentIntentId, amountInCents }) => {
        this.clientSecret = clientSecret;
        this.paymentIntentId = paymentIntentId;
        this.amountInCents = amountInCents;
        this.unitPriceInCents = amountInCents / this.quantity;
        this.isCreatingPayment = false;
        this.mountCardElement();
      },
      error: (error) => {
        this.isCreatingPayment = false;
        this.errorMessage = typeof error.error === 'string'
          ? error.error
          : error.error?.message ?? 'No se pudo iniciar el pago. Inténtalo de nuevo.';
      },
    });
  }

  async pay(): Promise<void> {
    if (!this.clientSecret || !this.paymentIntentId || !this.stripe || !this.card) {
      this.errorMessage = 'Inicia el pago antes de introducir la tarjeta.';
      return;
    }

    this.isPaying = true;
    this.errorMessage = '';
    this.successMessage = '';

    let paymentSucceeded = false;
    try {
      const result = await this.stripe.confirmCardPayment(this.clientSecret, {
        payment_method: { card: this.card },
      });

      if (result.error) {
        this.errorMessage = result.error.message;
      } else if (result.paymentIntent?.status === 'succeeded') {
        paymentSucceeded = true;
        const vehicles = await firstValueFrom(
          this.paymentService.completePurchase(this.quantity, this.paymentIntentId),
        );
        const registrations = vehicles.map((vehicle) => vehicle.matricula).join(', ');
        this.successMessage =
          `Compra completada: ${vehicles.length} bicicleta(s) asignada(s) a ${vehicles[0]?.municipio}. `
          + `Matrículas: ${registrations}.`;
        this.clientSecret = '';
        this.paymentIntentId = '';
        return;
      } else {
        this.errorMessage = 'El pago no se ha podido completar.';
      }
    } catch (error) {
      if (paymentSucceeded && error instanceof HttpErrorResponse) {
        this.errorMessage = typeof error.error === 'string'
          ? error.error
          : error.error?.message ?? error.error?.detail
            ?? 'El cobro se realizó pero no se pudo confirmar la asignación. Contacta con soporte.';
        if (error.status === 409) {
          this.clientSecret = '';
          this.paymentIntentId = '';
          this.cardComplete = false;
        }
      } else {
        this.errorMessage = 'No se pudo confirmar el pago. Inténtalo de nuevo.';
      }
    } finally {
      this.isPaying = false;
    }
  }

  private mountCardElement(): void {
    if (typeof Stripe === 'undefined') {
      this.errorMessage = 'No se pudo cargar Stripe. Recarga la página e inténtalo de nuevo.';
      return;
    }

    this.stripe ??= Stripe(environment.stripePublishableKey);
    if (!this.card) {
      this.card = this.stripe.elements().create('card');
      this.card.mount(this.cardElement.nativeElement);
      this.card.on('change', (event) => {
        this.cardComplete = event.complete;
        this.errorMessage = event.error?.message ?? '';
      });
    }
  }

  private formatEuro(amountInCents: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
    }).format(amountInCents / 100);
  }
}
