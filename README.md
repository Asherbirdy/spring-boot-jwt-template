# pos-cloud

### Roles
- ADMIN
- MANAGER
- STAFF

## STORE_ROLE
- STORE_MANAGER
- STORE_STAFF

### 啟動專案流程
- 創建TABLE至資料庫
- 打DEV_REGISTER 建立ADMIN帳號 member role=ADMIN 才能建立企業帳號

### 用 Docker 啟動

前置：複製 `.envSample` 為 `.env` 並填好 DB / Mail 設定。`DOCKER_DB_URL` 預設為 `jdbc:postgresql://host.docker.internal:5432/poscloud`，會在容器內覆蓋 `DB_URL`。

```bash
docker compose up --build      # 前景啟動並 build image
docker compose up --build -d   # 背景啟動
docker compose logs -f app     # 看 log
docker compose down            # 停止
```

啟動後 API 在 `http://localhost:8083`。

本機 Postgres 需允許從 Docker bridge 連入：
- `postgresql.conf`：`listen_addresses = '*'`
- `pg_hba.conf`：`host all all 172.17.0.0/16 md5`
- 改完重啟 Postgres（例如 `brew services restart postgresql`）

### 企業開戶流程
- ADMIN 幫忙申請 STORE
- ADMIN 幫忙 STORE 申請 STORE_MANAGER 帳號

### 開店流程
- STORE_MANAGER 幫忙 STORE_STAFF申請 開班帳號
- 開發者請 MANAGER 登入POS系統（需要EMAIL驗證）
- STORE_MANAGER  設定 STORE 可以同時開班的次數
- STORE_MANAGER  幫忙 STAFF創建帳號
- STORE_MANAGER 建立商品表單

### 開班
- 每個store都有自己獨立的帳號
- STAFF 登入帳號 
- STAFF 開班 (並記錄LOG)
- STAFF 關班 (並記錄LOG)

-----------
EMAIL 登入驗證：
admin 和 STORE_MANAGER 登入 需要email 驗證碼，STORE_STAFF不需要# spring-boot-jwt-template
