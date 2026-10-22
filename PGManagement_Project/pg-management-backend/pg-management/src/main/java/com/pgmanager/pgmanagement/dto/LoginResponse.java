package com.pgmanager.pgmanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long ownerId;
    private String name;
    private String email;
}