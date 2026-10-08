package com.banking.payment_service.service;

import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentOrderResponse;
import com.banking.payment_service.entity.Payment;
import com.banking.payment_service.entity.PaymentStatus;
import com.banking.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETE_TOPIC = "payment.complete";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    /**
     *  Create Razorpay payment order
     *
     *  FLOW:
     *  1. Create order in razorpay
     *  2. Save Payment record in DB
     *  3. Return order details to frontend
     *  4. Frontend show RazorPay Checkout
     *  5. User Pay
     *  6. Razorpay call webhook
     * @param request
     * @return
     * */

    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest
                                                   request) throws RazorpayException {
       log.info("Creating payment order for account: {} amount: {}",
               request.getAccountNumber(), request.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        // Converted amount
        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "USD/INR");
        orderRequest.put("receipt", "rect_" + System.currentTimeMillis() +  UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10));

        // Create order in razorpay
        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        log.info("Razorpay order created: {}", razorpayOrder.get("id").toString());

        //Save Payment record in DB
        Payment payment = new Payment();
        payment.setRazorpayPaymentId(razorpayOrder.get("id").toString());
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setCurrency("USD/INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrder.get("id").toString(),
                request.getAmount(),
                "USD/INR",
                "CREATED",
                keyId
        );
    }
}
