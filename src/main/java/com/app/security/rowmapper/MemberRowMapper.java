package com.app.security.rowmapper;

import com.app.security.model.Member;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MemberRowMapper implements RowMapper<Member> {

    @Override
    public Member mapRow(ResultSet resultSet, int i) throws SQLException {
        Member member = new Member();
        member.setMemberId(resultSet.getString("member_id"));
        member.setName(resultSet.getString("name"));
        member.setEmail(resultSet.getString("email"));
        member.setPassword(resultSet.getString("password"));
        member.setRole(resultSet.getString("role"));
        member.setCreatedAt(resultSet.getTimestamp("created_at"));
        member.setUpdatedAt(resultSet.getTimestamp("updated_at"));

        return member;
    }
}
