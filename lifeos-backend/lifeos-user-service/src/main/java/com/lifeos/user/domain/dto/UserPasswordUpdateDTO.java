package com.lifeos.user.domain.dto;

import lombok.Data;

@Data
public class UserPasswordUpdateDTO {
    private String currentPassword;
    private String newPassword;
}
