package com.saurabh.userDB.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;
@Data

@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserDB {
    @Id
    @Column(name = "user_id")
    private String userId;
    private String name;
    private String email;
    private String contact;
    private String dob;
    private String password;
    @Column(name = "pwd_retry")
    private Integer pwdRetry;

}
