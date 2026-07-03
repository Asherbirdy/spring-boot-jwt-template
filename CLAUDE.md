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

Spring Boot 3.1.5 app with layered architecture using Spring JDBC (not JPA):

```
Controller → Service (interface + impl) → DAO (interface + impl) → PostgreSQL
```

All code lives under `com.app.security`. DAO layer uses `NamedParameterJdbcTemplate` with raw SQL and `RowMapper` classes for result mapping.

### Model / RowMapper / schema.sql 同步規則

當改動到下列任一檔案時，必須同步檢查並更新另外兩個，保持三者一致：

- `src/main/java/com/app/security/model/*.java`（model 欄位、型別）
- `src/main/java/com/app/security/rowmapper/*.java`（DB 欄位 → model setter 的對應）
- `src/test/resources/schema.sql`（DB 表結構、欄位名、型別、約束）

慣例：DB 欄位用 snake_case（如 `product_category_id`），Java model 欄位用 camelCase（如 `productCategoryId`），RowMapper 負責橋接兩者。新增欄位、改名、改型別、加減約束時都要三邊一起改。

### DAO SQL 字串慣例

DAO 內所有 SQL 字串一律使用 Java text block (`"""..."""`)，不要用 `"..." + "..."` 字串串接或單行字串。需要動態插入欄位常數（例如 `COLUMNS`）時用 `.formatted(...)`。

範例：

```java
String sql = """
        UPDATE store_checkout
        SET order_status = :orderStatus,
            updated_at = NOW()
        WHERE store_checkout_id = :storeCheckoutId
        """;

String sql = """
        SELECT %s
        FROM store_checkout
        WHERE store_id = :storeId
        ORDER BY checkout_at DESC
        """.formatted(COLUMNS);
```

### Controller 註解排列慣例

Controller 方法上的註解一律將 Spring mapping 註解（`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` 等）放在最上方，權限相關註解（如 `@RequireStoreRole`）放在下方。

範例：

```java
@GetMapping("/")
@RequireStoreRole({StoreRole.STORE_MANAGER})
```

### Authentication & Security

- Stateless JWT auth, **前後端分離**：登入成功時於 `AuthLoginResponse.tokenPair` 回傳 `{ accessToken, refreshToken }`，前端自行存進非 HttpOnly cookie；後續請求由前端在 `Authorization: Bearer <accessToken>` header 帶上，後端不依賴 cookie 傳 token。
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
