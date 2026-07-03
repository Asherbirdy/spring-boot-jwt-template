package com.app.security.rowmapper;

import com.app.security.model.Token;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TokenRowMapper implements RowMapper<Token> {

    @Override
    public Token mapRow(ResultSet resultSet, int i) throws SQLException {
        Token token = new Token();
        token.setTokenId(resultSet.getString("token_id"));
        token.setRefreshToken(resultSet.getString("refresh_token"));
        token.setIp(resultSet.getString("ip"));
        token.setUserAgent(resultSet.getString("user_agent"));
        token.setIsValid(resultSet.getBoolean("is_valid"));
        token.setMemberId(resultSet.getString("member_id"));
        token.setCreatedAt(resultSet.getTimestamp("created_at"));
        token.setUpdatedAt(resultSet.getTimestamp("updated_at"));

        return token;
    }
}
