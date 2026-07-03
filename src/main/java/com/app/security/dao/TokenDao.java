package com.app.security.dao;

import com.app.security.model.Token;

public interface TokenDao {

    Token getValidTokenByMemberId(String memberId);

    String createToken(Token token);

    void deleteTokensByMemberId(String memberId);
}
