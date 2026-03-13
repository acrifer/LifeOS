CREATE TABLE IF NOT EXISTS `ai_workflow_job` (
    `id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `note_id` bigint NULL DEFAULT NULL,
    `job_type` varchar(32) NOT NULL,
    `status` varchar(20) NOT NULL,
    `request_payload` longtext NULL,
    `result_payload` longtext NULL,
    `error_message` varchar(500) NULL DEFAULT NULL,
    `finished_time` datetime NULL DEFAULT NULL,
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ai_job_user_time` (`user_id`, `create_time`) USING BTREE,
    INDEX `idx_ai_job_note_time` (`note_id`, `create_time`) USING BTREE,
    INDEX `idx_ai_job_type_status` (`job_type`, `status`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;
