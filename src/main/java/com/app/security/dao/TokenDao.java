package com.app.security.dao;

import com.app.security.model.Token;

import java.util.UUID;

public interface TokenDao {

    Token getValidTokenByMemberId(String memberId);

    /**
     * 在 Java 端產生 tokenId（UUID），寫入資料庫後回傳該 id。
     * MyBatis 的 insert 只回傳影響筆數，故 UUID 在此產生，再委派給 {@link #insertToken} 執行實際寫入。
     */
    default String createToken(Token token) {
        String tokenId = UUID.randomUUID().toString();
        token.setTokenId(tokenId);
        insertToken(token);
        return tokenId;
    }

    /** 對應 TokenMapper.xml 的實際 INSERT，由 {@link #createToken} 呼叫。 */
    void insertToken(Token token);

    void deleteTokensByMemberId(String memberId);
}
