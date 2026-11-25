package com.braille.braille_learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserSummary user;

    public LoginResponse(String token, UserSummary user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.user = user;
    }
}
