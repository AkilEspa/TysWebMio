package edu.uclm.esi.tysweb.beusu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.RefundCreateParams;
import java.util.Map;

@Service
public class PaymentService {
    private static final long MINIMUM_AMOUNT_CENTS = 50;
    private static final long MAXIMUM_AMOUNT_CENTS = 99_999_999;
    private static final int MAXIMUM_PURCHASE_QUANTITY = 100;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.rental.price-per-hour-cents:500}")
    private long rentalPricePerHourCents;

    @Value("${app.rental.max-hours:168}")
    private int maximumRentalHours;

    @Value("${app.purchase.price-per-vehicle-cents:10000}")
    private long purchasePricePerVehicleCents;

    public PaymentIntent createPurchasePaymentIntent(Integer quantity, String customerEmail, String municipality) {
        long amountInCents = this.purchaseAmountInCents(quantity);
        try {
            return this.createPaymentIntent(amountInCents, Map.of(
                    "purpose", "vehicle_purchase",
                    "quantity", quantity.toString(),
                    "customer_email", customerEmail,
                    "municipality", municipality));
        } catch (StripeException exception) {
            throw this.stripeException(exception);
        }
    }

    public boolean verifyPurchasePayment(
            String paymentIntentId, Integer quantity, String customerEmail, String municipality) {
        if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")
                || quantity == null || quantity < 1 || quantity > MAXIMUM_PURCHASE_QUANTITY) {
            return false;
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, this.requestOptions());
            Map<String, String> metadata = intent.getMetadata();
            return "succeeded".equals(intent.getStatus())
                    && intent.getAmount() == this.purchaseAmountInCents(quantity)
                    && "vehicle_purchase".equals(metadata.get("purpose"))
                    && !"true".equals(metadata.get("purchase_refunded"))
                    && quantity.toString().equals(metadata.get("quantity"))
                    && customerEmail.equals(metadata.get("customer_email"))
                    && municipality.equals(metadata.get("municipality"));
        } catch (StripeException exception) {
            throw this.stripeException(exception);
        }
    }

    public long purchaseAmountInCents(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAXIMUM_PURCHASE_QUANTITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una cantidad de bicicletas válida.");
        }
        try {
            long amount = Math.multiplyExact(purchasePricePerVehicleCents, quantity.longValue());
            if (amount < MINIMUM_AMOUNT_CENTS || amount > MAXIMUM_AMOUNT_CENTS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe de la compra no es válido.");
            }
            return amount;
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe de la compra no es válido.", exception);
        }
    }

    public void refundPurchasePayment(
            String paymentIntentId, Integer quantity, String customerEmail, String municipality) {
        if (!this.verifyPurchasePayment(paymentIntentId, quantity, customerEmail, municipality)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reembolsar un pago de compra no confirmado o no asociado a esta cuenta.");
        }

        try {
            RequestOptions refundOptions = RequestOptions.builder()
                    .setApiKey(stripeSecretKey)
                    .setIdempotencyKey("purchase-refund-" + paymentIntentId)
                    .build();
            Refund.create(
                    RefundCreateParams.builder()
                            .setPaymentIntent(paymentIntentId)
                            .build(),
                    refundOptions);
            PaymentIntent.retrieve(paymentIntentId, refundOptions).update(
                    PaymentIntentUpdateParams.builder()
                            .putMetadata("purchase_refunded", "true")
                            .build(),
                    refundOptions);
        } catch (StripeException exception) {
            throw this.stripeException(exception);
        }
    }

    public PaymentIntent createRentalPaymentIntent(Long vehicleId, Integer hours, String customerEmail) {
        if (vehicleId == null || vehicleId <= 0 || hours == null || hours < 1 || hours > maximumRentalHours) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un vehículo y una duración válida.");
        }

        long amountInCents;
        try {
            amountInCents = Math.multiplyExact(rentalPricePerHourCents, hours.longValue());
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe del alquiler no es válido.", exception);
        }
        if (amountInCents < MINIMUM_AMOUNT_CENTS || amountInCents > MAXIMUM_AMOUNT_CENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe del alquiler no es válido.");
        }

        try {
            return this.createPaymentIntent(amountInCents, Map.of(
                    "purpose", "vehicle_rental",
                    "vehicle_id", vehicleId.toString(),
                    "rental_hours", hours.toString(),
                    "customer_email", customerEmail));
        } catch (StripeException exception) {
            throw this.stripeException(exception);
        }
    }

    public boolean verifyRentalPayment(
            String paymentIntentId, Long vehicleId, Integer hours, String customerEmail) {
        if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")
                || vehicleId == null || hours == null || hours < 1 || hours > maximumRentalHours) {
            return false;
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, this.requestOptions());
            long expectedAmount = Math.multiplyExact(rentalPricePerHourCents, hours.longValue());
            Map<String, String> metadata = intent.getMetadata();
            return "succeeded".equals(intent.getStatus())
                    && intent.getAmount() == expectedAmount
                    && "vehicle_rental".equals(metadata.get("purpose"))
                    && vehicleId.toString().equals(metadata.get("vehicle_id"))
                    && hours.toString().equals(metadata.get("rental_hours"))
                    && customerEmail.equals(metadata.get("customer_email"));
        } catch (StripeException exception) {
            throw this.stripeException(exception);
        }
    }

    public long rentalAmountInCents(Integer hours) {
        if (hours == null || hours < 1 || hours > maximumRentalHours) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duración de alquiler no válida.");
        }
        try {
            return Math.multiplyExact(rentalPricePerHourCents, hours.longValue());
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Importe de alquiler no válido.", exception);
        }
    }

    public void refundRentalPayment(
            String paymentIntentId, Long vehicleId, Integer hours, String customerEmail) {
        if (!this.verifyRentalPayment(paymentIntentId, vehicleId, hours, customerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reembolsar un pago de alquiler no confirmado o no asociado a esta cuenta.");
        }

        try {
            RequestOptions refundOptions = RequestOptions.builder()
                    .setApiKey(stripeSecretKey)
                    .setIdempotencyKey("rental-refund-" + paymentIntentId)
                    .build();
            Refund.create(
                    RefundCreateParams.builder()
                            .setPaymentIntent(paymentIntentId)
                            .build(),
                    refundOptions);
        } catch (StripeException exception) {
            throw this.stripeException(exception);
        }
    }

    private PaymentIntent createPaymentIntent(long amountInCents, Map<String, String> metadata)
            throws StripeException {
        if (amountInCents < MINIMUM_AMOUNT_CENTS || amountInCents > MAXIMUM_AMOUNT_CENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Importe no válido.");
        }
        PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur");
        metadata.forEach(params::putMetadata);
        return PaymentIntent.create(params.build(), this.requestOptions());
    }

    private RequestOptions requestOptions() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe no está configurado.");
        }
        return RequestOptions.builder().setApiKey(stripeSecretKey).build();
    }

    private ResponseStatusException stripeException(StripeException exception) {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo completar la operación con Stripe.",
                exception);
    }
}
