package com.app.security.controller;

import com.app.security.dto.Member.MemberInfoResponse;
import com.app.security.dto.Response;
import com.app.security.service.AuthService;
import com.app.security.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    private final AuthService authService;

    public MemberController(MemberService memberService, AuthService authService) {
        this.memberService = memberService;
        this.authService = authService;
    }

    /**
     * 取得目前登入會員的個人資料。
     * 用於前端顯示登入狀態、個人資訊頁。
     */
    @GetMapping("/showMe")
    public Response<MemberInfoResponse> showMe() {
        MemberInfoResponse memberInfo = memberService.showMemberInfo();
        return new Response<>("Success", memberInfo, HttpStatus.OK);
    }

    /**
     * 登出：清除 token cookie 並使 refresh token 失效。
     * 用於使用者主動登出。
     */
    @PostMapping("/logout")
    public Response<Void> logout() {
        authService.logout();
        return new Response<>("LOGOUT_SUCCESS", null, HttpStatus.OK);
    }
}
