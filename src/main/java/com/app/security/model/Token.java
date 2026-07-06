package com.app.security.model;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
public class Token {

    private String tokenId;
    @ToString.Exclude
    private String refreshToken;
    private String ip;
    private String userAgent;
    private Boolean isValid;
    private String memberId;
    private Date createdAt;
    private Date updatedAt;
}
