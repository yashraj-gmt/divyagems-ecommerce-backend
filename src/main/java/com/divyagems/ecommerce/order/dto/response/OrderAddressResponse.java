package com.divyagems.ecommerce.order.dto.response;

import lombok.*;

/**
 * Immutable address snapshot returned in order responses.
 * Reflects the address as it was at order placement time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAddressResponse {

    private String firstName;
    private String lastName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pinCode;
    private String country;
}
