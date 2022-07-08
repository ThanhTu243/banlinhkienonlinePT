package com.thanhtu.crud.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdminsDto {
    private Integer adminId;
    private String userAdmin;
    private String firstName;
    private String lastName;
    private String lastnameAdmin;
    private String gmailAdmin;
}
