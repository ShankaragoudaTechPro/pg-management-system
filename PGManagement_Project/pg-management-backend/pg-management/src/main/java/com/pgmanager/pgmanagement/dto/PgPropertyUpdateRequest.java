package com.pgmanager.pgmanagement.dto.pg;

import com.pgmanager.pgmanagement.entity.PgType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PgPropertyUpdateRequest {

    @NotBlank(message = "PG name is required")
    @Size(max = 150, message = "PG name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "PG type is required")
    private PgType type;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}