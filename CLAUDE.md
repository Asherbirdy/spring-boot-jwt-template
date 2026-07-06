# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
mvn clean package              # Build project
mvn spring-boot:run            # Run application (port 8083)
mvn test                       # Run all tests
mvn clean package -DskipTests  # Build without tests
```

- Requires Java 17 and PostgreSQL
- Uses spring-dotenv: database config loaded from `.env` file (see `.envSample` for template)
- Test database: H2 in-memory (no external DB needed for tests)

## Architecture

Spring Boot 3.1.5 app with layered architecture using MyBatis 3 (not JPA, not raw Spring JDBC):

```
Controller → Service (interface + impl) → DAO (@Mapper 介面 + XML) → PostgreSQL
```

All code lives under `com.app.security`. DAO 層是純 Mapper 介面（`com.app.security.dao`），由啟動類的 `@MapperScan("com.app.security.dao")` 掃描，SQL 寫在 `src/main/resources/mapper/*.xml`。沒有 `DaoImpl`、沒有 `RowMapper` —— MyBatis 在執行期用動態代理實作介面，並靠 `mybatis.configuration.map-underscore-to-camel-case=true`（見 `application.properties`）自動把 snake_case 欄位對應到 camelCase 屬性。

### Model / XML Mapper / schema.sql 同步規則

當改動到下列任一檔案時，必須同步檢查並更新另外兩個，保持三者一致：

- `src/main/java/com/app/security/model/*.java`（model 欄位、型別）
- `src/main/resources/mapper/*.xml`（SQL 的 SELECT 欄位清單、INSERT/UPDATE 的 `#{...}` 屬性）
- `src/test/resources/schema.sql`（DB 表結構、欄位名、型別、約束）

慣例：DB 欄位用 snake_case（如 `product_category_id`），Java model 欄位用 camelCase（如 `productCategoryId`），MyBatis 的 underscore-to-camel-case 負責橋接兩者（不需手寫 resultMap）。新增欄位、改名、改型別、加減約束時都要三邊一起改。

### DAO / Mapper 慣例

- DAO 介面放 `com.app.security.dao`，方法對應 XML 中同名的 `<select>/<insert>/<update>/<delete>` id；XML 的 `namespace` 必須是介面全名。
- 方法有**多個參數**時，每個參數都要加 `@Param("xxx")`，XML 才能用 `#{xxx}` 對應；單一參數方法可省略。
- 參數綁定一律用 `#{...}`（PreparedStatement 佔位符，防 SQL injection），不要用 `${...}` 字串插值。
- 需要「Java 端產生 id（UUID）後回傳」的 insert：在介面用 `default` method 產生 UUID、set 進 model、再委派給對應 XML 的 `insertXxx`（回傳影響筆數），最後回傳 id。參考 `MemberDao.createMember` / `TokenDao.createToken`。
- 動態條件查詢優先用 MyBatis 標籤（`<if>`、`<foreach>`、`<choose>`），不要在 Java 端拼 SQL 字串。

範例：

```xml
<update id="updateRole">
    UPDATE member
    SET role = #{role},
        updated_at = NOW()
    WHERE member_id = #{memberId}
</update>
```

### Controller 註解排列慣例

Controller 方法上的註解一律將 Spring mapping 註解（`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` 等）放在最上方，權限相關註解（如 `@RequireStoreRole`）放在下方。

範例：

```java
@GetMapping("/")
@RequireStoreRole({StoreRole.STORE_MANAGER})
```

### Authentication & Security

- Stateless JWT auth：登入成功時後端把 `accessToken` / `refreshToken` 寫進 **HttpOnly + Secure + SameSite=None** cookie（見 `AuthServiceImpl.buildTokenCookie`），`AuthLoginResponse` 只回會員基本資料、**不含 token**。`Secure` 由 `.env` 的 `COOKIE_SECURE` 控制（`application.properties` 的 `cookie.secure=${COOKIE_SECURE:true}`，未設預設 true；本機純 http 開發設 false）。`JwtAuthenticationFilter` 讀取 token 時仍相容 `Authorization: Bearer <accessToken>` header，找不到才 fallback 讀 cookie。
- Access token (15 min) + refresh token (24 hr) stored in `token` table with IP/User-Agent tracking
- `JwtAuthenticationFilter` auto-refreshes expired access tokens using valid refresh tokens
- Route security in `MySecurityConfig`: `/auth/**` 與 `/dev/test` public、`/enterprise/**` 與 `/member-store-access/**` 需 `admin`、`/member/**`、`/storeShift/**`、`/product-category/**`、`/product-item/**`、`/logout` 需 authenticated，其餘 `denyAll`
- CORS: `MySecurityConfig.createCorsConfig()` 的 `setAllowedOrigins` 必須包含前端 dev server origin（預設 `http://localhost:1207`）；新增環境（staging/prod 等）時要在這裡補上對應 origin，否則前端會拿到 403 `Invalid CORS request`
- Passwords hashed with BCrypt

### Multi-Tenancy Model

- `member` → system-level users with global role (admin/user)
- `enterprise` → organizations
- `store` → retail locations under an enterprise
- `member_store_access` → per-store role assignment (STORE_MANAGER, STORE_STAFF) with status tracking
- One member can have access to multiple enterprises/stores

### Response Pattern

All endpoints return `Response<T>` which wraps `ApiResponse<T>` (message + data) in a `ResponseEntity`. Errors handled by `GlobalExceptionHandler`.

## Testing

- Integration tests use `@SpringBootTest` + `MockMvc` with H2 database
- `AuthTestSupport` base class provides pre-authenticated `adminAccessToken` and `userAccessToken` helpers
- Test data seeded via `src/test/resources/data.sql` (admin@gmail.com / user@gmail.com, password: "password")
- Test suite runner: `ApplicationTest.java` using JUnit 5 Platform Suite

### Test 註解慣例

每個 `@Test` 方法上方都要加一段 Javadoc 註解說明該測試在驗證什麼（前置條件、行為、預期結果），讓人不用讀內容就知道目的。

範例：

```java
/**
 * 驗證：openShifts 數量已達 running_devices_limit → 丟 ShiftLimitReachedException，
 * 且不應呼叫 dao.openShift。
 */
@Test
@DisplayName("openShift: 達到上限應丟 SHIFT_LIMIT_REACHED")
public void openShift_limitReached_throws() { ... }
```

## Roles

System roles: `admin`, `user` (stored in `member.role`)
Store-level roles: `store_manager`, `store_staff` (stored in `member_store_access.role`)
