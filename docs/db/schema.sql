CREATE TABLE `card` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `question` text NOT NULL,
  `answer` mediumtext NOT NULL,
  `created_at` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review_plan` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `card_id` int unsigned NOT NULL,
  `planned_on` date NOT NULL,
  `reviewed_on` date DEFAULT NULL,
  `rating` tinyint DEFAULT NULL,
  `interval_days` int NOT NULL DEFAULT '1',
  `repeats` int NOT NULL DEFAULT '0',
  `ease_factor` double NOT NULL DEFAULT '2.5',
  PRIMARY KEY (`id`),
  KEY `idx_planned_on` (`planned_on`),
  KEY `fk_plan_card` (`card_id`),
  CONSTRAINT `fk_plan_card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review_statistic` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `card_id` int unsigned NOT NULL,
  `reviewed_at` datetime NOT NULL,
  `duration_ms` int NOT NULL,
  `correct` tinyint(1) NOT NULL,
  `rating` tinyint NOT NULL,
  `notes` text,
  PRIMARY KEY (`id`),
  KEY `idx_stat_card` (`card_id`),
  CONSTRAINT `fk_stat_card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
