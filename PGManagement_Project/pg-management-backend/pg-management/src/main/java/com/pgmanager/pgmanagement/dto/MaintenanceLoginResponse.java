
package com.pgmanager.pgmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MaintenanceLoginResponse {

    private String token;
    private String tokenType;
    private Long staffId;
    private String name;
    private String email;
    private String role;
}
