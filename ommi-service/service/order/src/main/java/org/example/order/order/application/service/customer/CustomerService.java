package org.example.order.order.application.service.customer;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.AdminClient;
import org.example.customer.Customer;
import org.example.order.order.domain.order.model.MailingAddress;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final AdminClient adminClient;

    public Customer findById(int storeId, int customerId) {
        if (customerId == 0) return null;
        return withNotFoundHandler(() -> adminClient.customerGet(storeId, customerId));
    }

    private <T> T withNotFoundHandler(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw e;
        }
    }

    public Customer findByEmail(int storeId, String email) {
        if (StringUtils.isEmpty(email)) return null;
        return withNotFoundHandler(() -> adminClient.customerGetByEmail(email));
    }

    public Customer findByPhone(int storeId, String phone) {
        if (StringUtils.isEmpty(phone)) return null;
        return withNotFoundHandler(() -> adminClient.customerGetByPhone(phone));
    }

    public Customer create(int storeId, Pair<String, String> contact, Pair<String, String> fullName, MailingAddress address) {
        return null;
    }

    public Customer update(int storeId, int id, String finalEmail, String finalPhone) {
        return null;
    }
}
