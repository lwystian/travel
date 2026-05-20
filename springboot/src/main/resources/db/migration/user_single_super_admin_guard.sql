-- Enforce one and only one SUPER_ADMIN account at the database layer.
-- Run after reviewing which existing SUPER_ADMIN row should be kept.

UPDATE `user` u
JOIN (
    SELECT MIN(`id`) AS keep_id
    FROM `user`
    WHERE `role_code` = 'SUPER_ADMIN'
) k ON k.keep_id IS NOT NULL
SET u.`role_code` = 'ADMIN',
    u.`update_time` = NOW()
WHERE u.`role_code` = 'SUPER_ADMIN'
  AND u.`id` <> k.keep_id;

ALTER TABLE `user`
    ADD COLUMN `super_admin_guard` TINYINT
    GENERATED ALWAYS AS (
        CASE WHEN `role_code` = 'SUPER_ADMIN' THEN 1 ELSE NULL END
    ) STORED;

CREATE UNIQUE INDEX `uk_user_single_super_admin`
    ON `user` (`super_admin_guard`);
