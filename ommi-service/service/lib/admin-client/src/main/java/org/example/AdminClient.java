package org.example;

import feign.Param;
import feign.RequestLine;
import org.example.customer.Customer;
import org.example.location.Location;
import org.example.location.LocationFilter;
import org.example.product.OrderRouting;
import org.example.product.OrderRoutingRequest;
import org.example.product.response.Response;

import java.util.List;

public interface AdminClient {

    static AdminClientBuilder builder() {
        return new AdminClientBuilder();
    }

    @RequestLine("GET /admin/products/test")
    Response productTest();

    @RequestLine("GET /admin/customers/{storeId}/{id}")
    Customer customerGet(
            @Param("storeId") int storeId,
            @Param("id") int id
    );

    @RequestLine("GET /admin/customers/get_by_email?email={email}")
    Customer customerGetByEmail(
            @Param("email") String email);

    @RequestLine("GET /admin/customers/get_by_phone?phone={phone}")
    Customer customerGetByPhone(
            @Param("phone") String phone);

    List<OrderRouting> orderRouting(OrderRoutingRequest orderRoutingRequest, int storeId);

    @RequestLine("GET /admin/locations")
    List<Location> locationFilter(
            @Param("storeId") int storeId,
            LocationFilter locationFilter
    );
}
