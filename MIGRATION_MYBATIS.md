# Spring JDBC → MyBatis 3 遷移說明

本文件記錄把持久層從 **Spring JDBC (`NamedParameterJdbcTemplate` + `RowMapper`)** 遷移到 **MyBatis 3** 的完整改動，方便日後回顧與學習。

> 一句話總結：DAO 介面的方法簽章與行為保持不變，所以 **Service / Controller / Model / 測試 / schema.sql / data.sql 完全沒動**。

---

## 一、改動總覽

| 檔案 | 改了什麼 | 學習重點 |
|---|---|---|
| `pom.xml` | `spring-boot-starter-jdbc` → `mybatis-spring-boot-starter:3.0.3` | Spring Boot 3.x 只能配 MyBatis starter **3.0.x**（jakarta 命名空間），用到 2.x 會啟動失敗 |
| `Application.java` | 加 `@MapperScan("com.app.security.dao")` | MyBatis 掃描介面，執行期用**動態代理**自動生成實作，所以不用寫 `DaoImpl` |
| `dao/MemberDao.java` | 加 `@Param`、`createMember` 改 `default` method + 新增 `insertMember` | 多參數要 `@Param`；`default` method 解決「Java 產 UUID 並回傳」 |
| `dao/TokenDao.java` | `createToken` 改 `default` method + 新增 `insertToken` | 同上 |
| `src/main/resources/application.properties` | 加 `mybatis.mapper-locations` + `map-underscore-to-camel-case` | 後者自動 `member_id → memberId`，**取代 RowMapper** |
| `src/test/resources/application.properties` | 同上 | 測試環境也要設定 |
| `CLAUDE.md` | 慣例文字改成 MyBatis 版 | 三邊同步規則改成 Model / XML / schema.sql |

### 新增檔案
- `src/main/resources/mapper/MemberMapper.xml`
- `src/main/resources/mapper/TokenMapper.xml`

### 刪除檔案
- `dao/impl/MemberDaoImpl.java`
- `dao/impl/TokenDaoImpl.java`
- `rowmapper/MemberRowMapper.java`
- `rowmapper/TokenRowMapper.java`
- （連同空掉的 `dao/impl/`、`rowmapper/` 目錄）

---

## 二、架構前後對照

**遷移前：**
```
Controller → Service → DAO 介面 + DaoImpl(NamedParameterJdbcTemplate) + RowMapper → PostgreSQL
```

**遷移後：**
```
Controller → Service → DAO(@Mapper 介面) + XML Mapper → PostgreSQL
```

- 沒有 `DaoImpl`：MyBatis 執行期用動態代理實作介面。
- 沒有 `RowMapper`：靠 `map-underscore-to-camel-case=true` 自動把 snake_case 欄位對應到 camelCase 屬性。

---

## 三、MyBatis 核心語法對照

| 概念 | Spring JDBC（舊） | MyBatis（新） |
|---|---|---|
| 參數綁定 | `map.put("email", x)` + `:email` | 直接 `#{email}`（讀物件屬性 `getEmail()`） |
| 結果對應 | 手寫 `RowMapper` 類別 | `map-underscore-to-camel-case` 自動 |
| 查無資料 | `list.isEmpty() ? null : list.get(0)` | 回傳型別是單一物件時 MyBatis **自動回 null** |
| 動態 SQL | Java 端拼字串 | `<if>` / `<foreach>` / `<choose>` 標籤 |

### `#{}` vs `${}`（安全重點）
- **一律用 `#{}`**：PreparedStatement 佔位符，會參數化，防 SQL injection。
- `${}` 是字串直接插入 SQL，只在動態指定「欄位名 / 表名」時才用，且值**不能**來自使用者輸入。

---

## 四、關鍵細節：Java 產生 UUID 的 insert

`createMember` / `createToken` 舊版在 **Java 端** `UUID.randomUUID()` 產生 id 並回傳。
但 MyBatis 的 `<insert>` 只回傳「影響筆數」(int)，無法回傳 Java 產生的 UUID；
UUID 也不適合丟 SQL 產生（H2 用 `RANDOM_UUID()`、Postgres 用 `gen_random_uuid()`，函式不同、不可攜）。

**解法：介面的 `default` method。** 介面本身不能有邏輯，但 Java 8 的 `default` method 可以：

```java
default String createMember(Member member) {
    String memberId = UUID.randomUUID().toString();
    member.setMemberId(memberId);
    insertMember(member);   // 對應 XML 的實際 INSERT，回傳影響筆數
    return memberId;
}

void insertMember(Member member);
```

這樣 `createMember` 對外合約（回傳 String id）完全不變，Service 端零改動。

---

## 五、Model / XML Mapper / schema.sql 同步規則（新慣例）

改動下列任一檔案時，必須同步檢查另外兩個：

- `src/main/java/com/app/security/model/*.java`（model 欄位、型別）
- `src/main/resources/mapper/*.xml`（SELECT 欄位清單、INSERT/UPDATE 的 `#{...}` 屬性）
- `src/test/resources/schema.sql`（DB 表結構、欄位名、型別、約束）

慣例：DB 欄位用 snake_case（如 `product_category_id`），Java model 用 camelCase（如 `productCategoryId`），
MyBatis 的 underscore-to-camel-case 負責橋接，不需手寫 resultMap。

---

## 六、驗收

```bash
mvn clean test    # 若無 mvn：先 brew install maven
```

DAO 介面與行為未變，現有整合測試（`@SpringBootTest` + MockMvc + H2）應全數通過，無需修改測試。
