package com.app.security.model;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
public class Member {

    private String memberId;
    private String name;
    private String email;
    @ToString.Exclude
    private String password;
    private String role;
    private Date createdAt;
    private Date updatedAt;
}
