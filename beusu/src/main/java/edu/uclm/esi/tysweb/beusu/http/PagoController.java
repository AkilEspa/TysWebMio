package edu.uclm.esi.tysweb.beusu.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import edu.uclm.esi.tysweb.beusu.service.PaymentService;
import edu.uclm.esi.tysweb.beusu.service.UsersService;
import edu.uclm.esi.tysweb.beusu.dto.SessionResponse;
import com.stripe.model.PaymentIntent;
import org.springframework.web.server.ResponseStatusException;

record RentalPaymentRequest(Long vehicleId, Integer hours) {}
record RentalPaymentVerificationRequest(String paymentIntentId, Long vehicleId, Integer hours) {}
record RentalPaymentIntentResponse(String clientSecret, String paymentIntentId, long amountInCents) {}
record RentalPaymentVerificationResponse(boolean valid, String customerEmail, long amountInCents) {}
record PurchasePaymentRequest(Integer quantity) {}
record PurchasePaymentVerificationRequest(String paymentIntentId, Integer quantity) {}
record PurchasePaymentIntentResponse(String clientSecret, String paymentIntentId, long amountInCents) {}
record PurchasePaymentVerificationResponse(
        boolean valid, String customerEmail, String municipality, long amountInCents) {}

@RestController
@RequestMapping("/payments")
@CrossOrigin(
        origins = {
                "${app.frontend.url:http://localhost:4200}",
                "${app.frontend.secondary-url:http://localhost:4201}"
        },
        allowCredentials = "true")
public class PagoController {
    private final PaymentService paymentService;

    @Autowired
    private UsersService usersService;

    public PagoController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/purchase-intent")
    public ResponseEntity<PurchasePaymentIntentResponse> purchaseIntent(
            @RequestBody PurchasePaymentRequest request,
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        SessionResponse user = this.authenticatedUser(cookieToken, headerToken);
        PaymentIntent intent = this.paymentService.createPurchasePaymentIntent(
                request.quantity(), user.email(), user.municipio());
        return ResponseEntity.ok(new PurchasePaymentIntentResponse(
                intent.getClientSecret(), intent.getId(), intent.getAmount()));
    }

    @PostMapping("/verify-purchase")
    public ResponseEntity<PurchasePaymentVerificationResponse> verifyPurchasePayment(
            @RequestBody PurchasePaymentVerificationRequest request,
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        SessionResponse user = this.authenticatedUser(cookieToken, headerToken);
        boolean valid = this.paymentService.verifyPurchasePayment(
                request.paymentIntentId(), request.quantity(), user.email(), user.municipio());
        long amountInCents = valid ? this.paymentService.purchaseAmountInCents(request.quantity()) : 0;
        return ResponseEntity.ok(new PurchasePaymentVerificationResponse(
                valid, user.email(), user.municipio(), amountInCents));
    }

    @PostMapping("/refund-purchase")
    public ResponseEntity<Void> refundPurchasePayment(
            @RequestBody PurchasePaymentVerificationRequest request,
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        SessionResponse user = this.authenticatedUser(cookieToken, headerToken);
        this.paymentService.refundPurchasePayment(
                request.paymentIntentId(), request.quantity(), user.email(), user.municipio());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rental-intent")
    public ResponseEntity<RentalPaymentIntentResponse> rentalIntent(
            @RequestBody RentalPaymentRequest request,
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        SessionResponse user = this.authenticatedUser(cookieToken, headerToken);
        PaymentIntent intent = this.paymentService.createRentalPaymentIntent(
                request.vehicleId(), request.hours(), user.email());
        return ResponseEntity.ok(new RentalPaymentIntentResponse(
                intent.getClientSecret(), intent.getId(), intent.getAmount()));
    }

    @PostMapping("/verify-rental")
    public ResponseEntity<RentalPaymentVerificationResponse> verifyRentalPayment(
            @RequestBody RentalPaymentVerificationRequest request,
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        SessionResponse user = this.authenticatedUser(cookieToken, headerToken);
        boolean valid = this.paymentService.verifyRentalPayment(
                request.paymentIntentId(), request.vehicleId(), request.hours(), user.email());
        long amountInCents = valid ? this.paymentService.rentalAmountInCents(request.hours()) : 0;
        return ResponseEntity.ok(new RentalPaymentVerificationResponse(valid, user.email(), amountInCents));
    }

    @PostMapping("/refund-rental")
    public ResponseEntity<Void> refundRentalPayment(
            @RequestBody RentalPaymentVerificationRequest request,
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        SessionResponse user = this.authenticatedUser(cookieToken, headerToken);
        this.paymentService.refundRentalPayment(
                request.paymentIntentId(), request.vehicleId(), request.hours(), user.email());
        return ResponseEntity.noContent().build();
    }

    private SessionResponse authenticatedUser(String cookieToken, String headerToken) {
        String token = headerToken != null ? headerToken : cookieToken;
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return this.usersService.validateSession(token);
    }
}