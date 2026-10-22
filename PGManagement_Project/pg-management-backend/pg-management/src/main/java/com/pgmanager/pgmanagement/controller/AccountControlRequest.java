
package com.pgmanager.pgmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountControlRequest {

    @NotBlank(message = "A reason is required")
    @Size(
            max = 500,
            message = "Reason cannot exceed 500 characters"
    )
    private String reason;
}
