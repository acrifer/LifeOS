ALTER TABLE `user_behavior`
    ADD COLUMN `event_id` varchar(64) NULL AFTER `id`;

UPDATE `user_behavior`
SET `event_id` = CONCAT('legacy-', `id`)
WHERE `event_id` IS NULL;

ALTER TABLE `user_behavior`
    MODIFY COLUMN `event_id` varchar(64) NOT NULL,
    ADD UNIQUE INDEX `uk_event_id` (`event_id`);
