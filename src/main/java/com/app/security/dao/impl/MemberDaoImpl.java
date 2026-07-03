package com.app.security.dao.impl;

import com.app.security.dao.MemberDao;
import com.app.security.model.Member;
import com.app.security.rowmapper.MemberRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MemberDaoImpl implements MemberDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final MemberRowMapper memberRowMapper;

    public MemberDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate, MemberRowMapper memberRowMapper) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.memberRowMapper = memberRowMapper;
    }

    @Override
    public Member getMemberByEmail(String email) {
        String sql = """
                SELECT member_id, name, email, password, role, created_at, updated_at
                FROM member
                WHERE email = :email
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("email", email);

        List<Member> memberList = namedParameterJdbcTemplate.query(sql, map, memberRowMapper);

        if (memberList.isEmpty()) {
            return null;
        }

        return memberList.get(0);

    }

    @Override
    public Member getMemberById(String memberId) {
        String sql = """
                SELECT member_id, name, email, password, role, created_at, updated_at
                FROM member
                WHERE member_id = :memberId
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);

        List<Member> memberList = namedParameterJdbcTemplate.query(sql, map, memberRowMapper);

        if (memberList.isEmpty()) {
            return null;
        }

        return memberList.get(0);

    }

    @Override
    public String createMember(Member member) {
        String memberId = UUID.randomUUID().toString();

        String sql = """
                INSERT INTO member(member_id, name, email, password, role, created_at, updated_at)
                VALUES (:memberId, :name, :email, :password, :role, NOW(), NOW())
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("name", member.getName());
        map.put("email", member.getEmail());
        map.put("password", member.getPassword());
        map.put("role", member.getRole() != null ? member.getRole() : "user");

        namedParameterJdbcTemplate.update(sql, map);

        return memberId;
    }

    @Override
    public List<Member> getAllMembers() {
        String sql = """
                SELECT member_id, name, email, password, role, created_at, updated_at
                FROM member
                ORDER BY created_at DESC
                """;

        return namedParameterJdbcTemplate.query(sql, new HashMap<>(), memberRowMapper);
    }

    @Override
    public List<Member> getRoleMembers(String role) {
        String sql = """
                SELECT member_id, name, email, password, role, created_at, updated_at
                FROM member
                WHERE role = :role
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("role", role);

        return namedParameterJdbcTemplate.query(sql, map, memberRowMapper);
    }

    @Override
    public void updateRole(String memberId, String role) {
        String sql = """
                UPDATE member
                SET role = :role,
                    updated_at = NOW()
                WHERE member_id = :memberId
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("role", role);

        namedParameterJdbcTemplate.update(sql, map);
    }
}
