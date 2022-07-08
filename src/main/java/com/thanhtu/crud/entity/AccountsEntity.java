package com.thanhtu.crud.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import javax.persistence.*;
import java.util.Collection;


@Entity
@Table(name="accounts")
@Getter
@Setter
@ToString
public class AccountsEntity {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;
    private String username;
    private String passwords;
    private String gmail;
    private String firstname;
    private String lastname;
    private String address;
    private String phonenumber;
    private String activationCode;
    private String passwordresetCode;
    private String activeAccount;
    private String provider;
    private String roles;
}
