package com.banking.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    /**
     * Consumes transaction.completed events.
     * Credits the receiver's account.
     */
    @KafkaListener(topics = "transaction.completed", groupId = "account-service-group")
    public void consumeTransactionComplete(@Payload Map<String, Object> payload) {

        try {
            String receiverAccount = (String) payload.get("receiverAccount");

            Object amountValue = payload.get("amount");

            if (receiverAccount == null || amountValue == null) {
                throw new IllegalArgumentException("Missing receiverAccount or amount in event");
            }

            BigDecimal amount = new BigDecimal(amountValue.toString());

            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Transaction amount must be positive");
            }

            log.info("Crediting account: {}, amount: {}", receiverAccount, amount);

            accountService.creditBalance(receiverAccount, amount);

        } catch (Exception e) {
            log.error("Failed to process transaction.completed event: {}", payload, e);

            throw e;
        }
    }

    /**
     * Consumes fraud.detected events.
     * Blocks the flagged account.
     */
    @KafkaListener(topics = "fraud.detected", groupId = "account-service-group")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {

        try {
            String accountNumber = (String) payload.get("accountNumber");

            if (accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("Missing accountNumber in fraud event");
            }

            log.warn("Fraud detected - blocking account: {}", accountNumber);

            accountService.blockAccount(accountNumber);

        } catch (Exception e) {
            log.error("Failed to process fraud.detected event: {}", payload, e);

            throw e;
        }
    }
}