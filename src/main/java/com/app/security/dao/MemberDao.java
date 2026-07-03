package com.app.security.dao;

import com.app.security.model.Member;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface MemberDao {

    Member getMemberByEmail(String email);

    Member getMemberById(String memberId);

    /**
     * 在 Java 端產生 memberId（UUID），寫入資料庫後回傳該 id。
     * MyBatis 的 insert 只回傳影響筆數，故 UUID 在此產生，再委派給 {@link #insertMember} 執行實際寫入。
     */
    default String createMember(Member member) {
        String memberId = UUID.randomUUID().toString();
        member.setMemberId(memberId);
        insertMember(member);
        return memberId;
    }

    /** 對應 MemberMapper.xml 的實際 INSERT，由 {@link #createMember} 呼叫。 */
    void insertMember(Member member);

    List<Member> getRoleMembers(String role);

    List<Member> getAllMembers();

    void updateRole(@Param("memberId") String memberId, @Param("role") String role);
}
