package com.app.security.dao.impl;

import com.app.security.dao.TokenDao;
import com.app.security.model.Token;
import com.app.security.rowmapper.TokenRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TokenDaoImpl implements TokenDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final TokenRowMapper tokenRowMapper;

    public TokenDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate, TokenRowMapper tokenRowMapper) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.tokenRowMapper = tokenRowMapper;
    }

    @Override
    public Token getValidTokenByMemberId(String memberId) {
        String sql = """
                SELECT token_id, refresh_token, ip, user_agent, is_valid, member_id, created_at, updated_at
                FROM token
                WHERE member_id = :memberId AND is_valid = true
                LIMIT 1
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);

        List<Token> tokenList = namedParameterJdbcTemplate.query(sql, map, tokenRowMapper);

        if (tokenList.isEmpty()) {
            return null;
        }

        return tokenList.get(0);

    }

    @Override
    public String createToken(Token token) {
        String tokenId = UUID.randomUUID().toString();

        String sql = """
                INSERT INTO token(token_id, refresh_token, ip, user_agent, is_valid, member_id, created_at, updated_at)
                VALUES (:tokenId, :refreshToken, :ip, :userAgent, :isValid, :memberId, NOW(), NOW())
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("tokenId", tokenId);
        map.put("refreshToken", token.getRefreshToken());
        map.put("ip", token.getIp());
        map.put("userAgent", token.getUserAgent());
        map.put("isValid", token.getIsValid());
        map.put("memberId", token.getMemberId());

        namedParameterJdbcTemplate.update(sql, map);

        return tokenId;
    }

    @Override
    public void deleteTokensByMemberId(String memberId) {
        String sql = """
                DELETE FROM token
                WHERE member_id = :memberId
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);

        namedParameterJdbcTemplate.update(sql, map);
    }
}
