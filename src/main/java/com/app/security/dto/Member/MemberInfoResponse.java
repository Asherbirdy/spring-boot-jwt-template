package com.app.security.dto.Member;

import java.util.Date;

public class MemberInfoResponse {
    private final String memberId;
    private final String name;
    private final String email;
    private final String role;
    private final Date createdAt;
    private final Date updatedAt;

    public MemberInfoResponse(String memberId, String name, String email, String role,
                               Date createdAt, Date updatedAt) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
