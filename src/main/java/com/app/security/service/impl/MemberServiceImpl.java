package com.app.security.service.impl;

import com.app.security.dao.MemberDao;
import com.app.security.dto.Member.MemberInfoResponse;
import com.app.security.model.Member;
import com.app.security.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MemberServiceImpl implements MemberService {

    private final MemberDao memberDao;

    public MemberServiceImpl(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public MemberInfoResponse showMemberInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberId = (String) authentication.getCredentials();
        Member member = memberDao.getMemberById(memberId);

        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NON_EXISTENT_MEMBER");
        }

        return new MemberInfoResponse(
                member.getMemberId(),
                member.getName(),
                member.getEmail(),
                member.getRole(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
