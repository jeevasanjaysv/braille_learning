package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummary {

    private Long id;
    private String name;
    private String email;

    public static UserSummary fromUser(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail());
    }
}
