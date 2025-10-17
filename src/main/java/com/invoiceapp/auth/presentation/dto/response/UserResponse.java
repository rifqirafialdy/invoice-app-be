package com.invoiceapp.auth.presentation.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private UUID userId;
    private String email;
    private String name;
    private String companyName;
    private String phone;
    private String address;
    private String logoUrl;
    private Boolean isVerified;
}
