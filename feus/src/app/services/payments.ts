import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export interface RentalPaymentIntent {
  clientSecret: string;
  paymentIntentId: string;
  amountInCents: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.beusuUrl}/payments`;

  createRentalIntent(vehicleId: number, hours: number) {
    return this.http.post<RentalPaymentIntent>(
      `${this.apiUrl}/rental-intent`,
      { vehicleId, hours },
      { withCredentials: true },
    );
  }
}
