ALTER TABLE `note_0`
    ADD COLUMN `pinned` tinyint(1) NOT NULL DEFAULT 0,
    ADD COLUMN `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN `next_review_at` datetime NULL DEFAULT NULL,
    ADD COLUMN `last_reviewed_at` datetime NULL DEFAULT NULL,
    ADD INDEX `idx_review_state` (`review_state`),
    ADD INDEX `idx_next_review_at` (`next_review_at`);

ALTER TABLE `note_1`
    ADD COLUMN `pinned` tinyint(1) NOT NULL DEFAULT 0,
    ADD COLUMN `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN `next_review_at` datetime NULL DEFAULT NULL,
    ADD COLUMN `last_reviewed_at` datetime NULL DEFAULT NULL,
    ADD INDEX `idx_review_state` (`review_state`),
    ADD INDEX `idx_next_review_at` (`next_review_at`);

ALTER TABLE `note_2`
    ADD COLUMN `pinned` tinyint(1) NOT NULL DEFAULT 0,
    ADD COLUMN `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN `next_review_at` datetime NULL DEFAULT NULL,
    ADD COLUMN `last_reviewed_at` datetime NULL DEFAULT NULL,
    ADD INDEX `idx_review_state` (`review_state`),
    ADD INDEX `idx_next_review_at` (`next_review_at`);

ALTER TABLE `note_3`
    ADD COLUMN `pinned` tinyint(1) NOT NULL DEFAULT 0,
    ADD COLUMN `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN `next_review_at` datetime NULL DEFAULT NULL,
    ADD COLUMN `last_reviewed_at` datetime NULL DEFAULT NULL,
    ADD INDEX `idx_review_state` (`review_state`),
    ADD INDEX `idx_next_review_at` (`next_review_at`);

ALTER TABLE `task`
    ADD COLUMN `source_note_id` bigint NULL DEFAULT NULL,
    ADD INDEX `idx_source_note_id` (`source_note_id`);
