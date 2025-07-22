DROP TRIGGER IF EXISTS prevent_insert_owner_role;
CREATE TRIGGER prevent_insert_owner_role
BEFORE INSERT ON user_roles
FOR EACH ROW
BEGIN
    DECLARE owner_id INT;
    SELECT id INTO owner_id FROM users WHERE email = 'owner@xoxo.com' LIMIT 1;
    IF NEW.role = 'OWNER' AND NEW.user_id <> owner_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Chỉ user gốc mới được phép có role OWNER!';
    END IF;
END;