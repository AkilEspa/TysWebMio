import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentService } from './payments';
import { environment } from '../../environments/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaymentService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('creates an authenticated purchase intent with the requested quantity', () => {
    const intent = {
      clientSecret: 'pi_test_secret',
      paymentIntentId: 'pi_test',
      amountInCents: 10000,
    };

    service.createPurchaseIntent(1).subscribe((response) => {
      expect(response).toEqual(intent);
    });

    const request = httpTestingController.expectOne(`${environment.beusuUrl}/payments/purchase-intent`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ quantity: 1 });
    expect(request.request.withCredentials).toBe(true);
    request.flush(intent);
  });

  it('finalizes the paid purchase through the rentals backend', () => {
    const vehicles = [{ id: 1, matricula: '0001', municipio: 'Albacete' }];

    service.completePurchase(1, 'pi_test').subscribe((response) => {
      expect(response).toEqual(vehicles);
    });

    const request = httpTestingController.expectOne(`${environment.bealquileresUrl}/vehiculos/comprar`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ quantity: 1, paymentIntentId: 'pi_test' });
    expect(request.request.withCredentials).toBe(true);
    request.flush(vehicles);
  });
});
