import { AsyncPipe, CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';
import { PaymentService, RentalPaymentIntent } from '../services/payments';
import { AvailableVehicle, Rental, RentalsService } from '../services/rentals';
import { Usuario } from '../services/usuario';

interface StripeCardChangeEvent {
  complete: boolean;
  error?: { message: string };
}

interface StripeCardElement {
  mount(element: HTMLElement): void;
  unmount(): void;
  on(event: 'change', handler: (event: StripeCardChangeEvent) => void): void;
}

interface StripeClient {
  elements(): { create(type: 'card'): StripeCardElement };
  confirmCardPayment(
    clientSecret: string,
    paymentData: { payment_method: { card: StripeCardElement } },
  ): Promise<{ error?: { message: string }; paymentIntent?: { status: string } }>;
}

declare const Stripe: (publishableKey: string) => StripeClient;

@Component({
  selector: 'app-rentals',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe, AsyncPipe],
  templateUrl: './rentals.html',
  styleUrl: './rentals.scss',
})
export class RentalsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('cardElement')
  private cardElement?: ElementRef<HTMLDivElement>;

  private readonly rentalsService = inject(RentalsService);
  private readonly paymentService = inject(PaymentService);
  readonly session$ = inject(Usuario).session$;
  private readonly cdr = inject(ChangeDetectorRef);
  private stripe?: StripeClient;
  private card?: StripeCardElement;
  private mountedCardElement?: HTMLDivElement;

  vehicles: AvailableVehicle[] = [];
  rentals: Rental[] = [];
  selectedVehicleId: number | null = null;
  hours = 1;
  checkout: RentalPaymentIntent | null = null;
  cardComplete = false;
  isLoading = false;
  isPreparingPayment = false;
  isPaying = false;
  errorMessage = '';
  successMessage = '';
  cardError = '';

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (!environment.stripePublishableKey) {
      this.errorMessage = 'Falta configurar la clave pública de Stripe.';
    }
  }

  ngOnDestroy(): void {
    this.card?.unmount();
  }

  chooseVehicle(vehicle: AvailableVehicle): void {
    if (this.isPreparingPayment || this.isPaying) return;
    this.selectedVehicleId = vehicle.id;
    this.checkout = null;
    this.cardComplete = false;
    this.cardError = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  startCheckout(): void {
    if (this.isPreparingPayment || this.isPaying) return;
    if (!this.selectedVehicleId || !Number.isInteger(this.hours) || this.hours < 1 || this.hours > 168) {
      this.errorMessage = 'Selecciona un vehículo y una duración entre 1 y 168 horas.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.checkout = null;
    this.isPreparingPayment = true;
    this.paymentService.createRentalIntent(this.selectedVehicleId, this.hours).subscribe({
      next: (intent) => {
        this.checkout = intent;
        this.isPreparingPayment = false;
        this.cdr.detectChanges();
        this.mountCardElement();
      },
      error: (error: HttpErrorResponse) => {
        this.isPreparingPayment = false;
        this.errorMessage = this.readError(error, 'No se pudo preparar el pago.');
      },
    });
  }

  async payAndRent(): Promise<void> {
    if (!this.checkout || !this.stripe || !this.card || !this.selectedVehicleId) {
      this.errorMessage = 'Prepara el alquiler antes de introducir los datos de pago.';
      return;
    }

    const intent = this.checkout;
    const vehicleId = this.selectedVehicleId;
    const hours = this.hours;
    this.isPaying = true;
    this.errorMessage = '';
    this.successMessage = '';

    let result: Awaited<ReturnType<StripeClient['confirmCardPayment']>>;
    try {
      result = await this.stripe.confirmCardPayment(intent.clientSecret, {
        payment_method: { card: this.card },
      });
      if (result.error) {
        this.errorMessage = result.error.message;
        this.isPaying = false;
        return;
      }
      if (result.paymentIntent?.status !== 'succeeded') {
        this.errorMessage = 'El pago no se ha podido completar.';
        this.isPaying = false;
        return;
      }
    } catch {
      this.errorMessage = 'No se pudo confirmar el pago. Inténtalo de nuevo.';
      this.isPaying = false;
      return;
    }

    this.rentalsService.createRental({
      vehicleId,
      hours,
      paymentIntentId: intent.paymentIntentId,
    }).subscribe({
      next: (rental) => {
        this.successMessage = `Alquiler confirmado: ${rental.matricula} durante ${rental.horas} hora(s).`;
        this.checkout = null;
        this.cardComplete = false;
        this.isPaying = false;
        this.loadData();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(
          error,
          'El pago se ha completado, pero no se pudo registrar el alquiler. Contacta con soporte.',
        );
        this.isPaying = false;
        this.cdr.markForCheck();
      },
    });
  }

  private loadData(): void {
    this.isLoading = true;
    this.rentalsService.getAvailableVehicles().subscribe({
      next: (vehicles) => {
        this.vehicles = vehicles;
        if (this.selectedVehicleId && !vehicles.some((vehicle) => vehicle.id === this.selectedVehicleId)) {
          this.selectedVehicleId = null;
          this.checkout = null;
        }
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudieron cargar los vehículos disponibles.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
    this.rentalsService.getMyRentals().subscribe({
      next: (rentals) => {
        this.rentals = rentals;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudo consultar tu historial de alquileres.');
        this.cdr.markForCheck();
      },
    });
  }

  private mountCardElement(): void {
    if (typeof Stripe === 'undefined') {
      this.errorMessage = 'No se pudo cargar Stripe. Recarga la página e inténtalo de nuevo.';
      return;
    }

    this.stripe ??= Stripe(environment.stripePublishableKey);
    if (!this.card) {
      this.card = this.stripe.elements().create('card');
      this.card.on('change', (event) => {
        this.cardComplete = event.complete;
        this.cardError = event.error?.message ?? '';
        this.cdr.markForCheck();
      });
    }
    const target = this.cardElement?.nativeElement;
    if (!target) {
      this.errorMessage = 'No se pudo preparar el formulario de tarjeta.';
      return;
    }
    if (this.mountedCardElement !== target) {
      if (this.mountedCardElement) this.card.unmount();
      this.card.mount(target);
      this.mountedCardElement = target;
    }
  }

  private readError(error: HttpErrorResponse, fallback: string): string {
    return typeof error.error === 'string'
      ? error.error
      : error.error?.message ?? error.error?.detail ?? fallback;
  }
}
