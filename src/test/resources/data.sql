-- 密碼的值為 password
INSERT INTO member (member_id, name, email, password, role, created_at, updated_at)
VALUES
--     ADMIN ACCOUNT
    ('e00324b8-8c07-481b-8bce-dc4242c6678a','AdminAccount','admin@gmail.com','$2a$10$ohFjCt/VDyIg2BKr/xZK8O5CjzwC9sySrcnbyQk81bmoyxN5/lE6u','admin','2026-04-22 10:40:56.196950','2026-04-22 10:40:56.196950'),
--     USER ACCOUNT
    ('92c14284-b0a8-4e2b-8dce-1f6e918b8374','UserAccount','user@gmail.com','$2a$10$0TCVUMx3FEMVFH7ztcFofuHfaFVt/E2eopwIG2bA6iyoau2va3px.','user','2026-04-22 10:40:56.196950','2026-04-22 10:40:56.196950');

INSERT INTO token (token_id, refresh_token, ip, user_agent,is_valid, member_id , created_at, updated_at)
VALUES
--     ADMIN TOKEN
    ('bb91f64c-0948-4d7d-aa2e-43808bd66316','a437aefe-bfc4-4ae9-99ed-f2c46b296f86','0:0:0:0:0:0:0:1','PostmanRuntime/7.53.0',true,'e00324b8-8c07-481b-8bce-dc4242c6678a',now(),now()),
--     USER TOKEN
    ('c417976f-cfaa-4ffa-820a-53788653db5f','59e40214-5297-4d0f-9e91-bbf7fe32455c','0:0:0:0:0:0:0:1','PostmanRuntime/7.53.0',true,'92c14284-b0a8-4e2b-8dce-1f6e918b8374',now(),now());
