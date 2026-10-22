package com.pgmanager.pgmanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FacilityResponse {

    private Long id;

    private String name;

    private boolean active;
}