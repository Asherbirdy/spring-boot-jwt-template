package com.app.security.dao;

import com.app.security.model.Member;

import java.util.List;

public interface MemberDao {

    Member getMemberByEmail(String email);

    Member getMemberById(String memberId);

    String createMember(Member member);

    List<Member> getRoleMembers(String role);

    List<Member> getAllMembers();

    void updateRole(String memberId, String role);
}
