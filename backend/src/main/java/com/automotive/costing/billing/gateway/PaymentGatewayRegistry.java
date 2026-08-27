package com.automotive.costing.billing.gateway;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGateway> byCode;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.byCode = gateways.stream().collect(Collectors.toMap(PaymentGateway::gatewayCode, Function.identity()));
    }

    public PaymentGateway require(String gatewayCode) {
        PaymentGateway g = byCode.get(gatewayCode);
        if (g == null) {
            throw new BusinessRuleViolationException("Unknown payment gateway: " + gatewayCode, "BILLING-01");
        }
        return g;
    }
}
