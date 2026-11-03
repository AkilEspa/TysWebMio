import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export interface PurchaseIntentResponse {
  clientSecret: string;
  paymentIntentId: string;
  amountInCents: number;
}

export interface PurchasedVehicle {
  id: number;
  matricula: string;
  municipio: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.beusuUrl}/payments`;

  createPurchaseIntent(quantity: number) {
    return this.http.post<PurchaseIntentResponse>(
      `${this.apiUrl}/purchase-intent`,
      { quantity },
      { withCredentials: true },
    );
  }

  completePurchase(quantity: number, paymentIntentId: string) {
    return this.http.post<PurchasedVehicle[]>(
      `${environment.bealquileresUrl}/vehiculos/comprar`,
      { quantity, paymentIntentId },
      { withCredentials: true },
    );
  }
}
