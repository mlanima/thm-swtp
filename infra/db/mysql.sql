-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
SHOW WARNINGS;
-- -----------------------------------------------------
-- Schema swtp
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `swtp` ;

-- -----------------------------------------------------
-- Schema swtp
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `swtp` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
SHOW WARNINGS;
USE `swtp` ;

-- -----------------------------------------------------
-- Table `user_profiles`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `user_profiles` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `user_profiles` (
  `keycloak_id` BINARY(16) NOT NULL,
  `about` TEXT NULL DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `experience` TEXT NULL DEFAULT NULL,
  `followers` INT NULL DEFAULT '0',
  `location` VARCHAR(255) NULL DEFAULT NULL,
  `title` VARCHAR(255) NULL DEFAULT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `username` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`keycloak_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE UNIQUE INDEX `UK5vlt12tabpccuckq0e84nhs4c` ON `user_profiles` (`username` ASC) VISIBLE;

SHOW WARNINGS;
CREATE UNIQUE INDEX `UKdqltqkaw58m11jbov0udx8xqg` ON `user_profiles` (`email` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `projects`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `projects` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `projects` (
  `id` BINARY(16) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `delete_at` DATETIME(6) NULL DEFAULT NULL,
  `description` VARCHAR(500) NULL DEFAULT NULL,
  `is_private` BIT(1) NOT NULL,
  `name` VARCHAR(20) NOT NULL,
  `project_url` VARCHAR(30) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `owner_keycloak_id` BINARY(16) NOT NULL,
  `open_positions_count` INT NOT NULL DEFAULT '0',
  `allow_join_requests` TINYINT(1) NOT NULL DEFAULT '1',
  `short_description` VARCHAR(200) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FKbtwdhh5lwvn8319t9ifjk72u0`
    FOREIGN KEY (`owner_keycloak_id`)
    REFERENCES `user_profiles` (`keycloak_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FKbtwdhh5lwvn8319t9ifjk72u0` ON `projects` (`owner_keycloak_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_favorites`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_favorites` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_favorites` (
  `id` BINARY(16) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `project_id` BINARY(16) NOT NULL,
  `user_keycloak_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FKemcvwka82bmpqhifol4hp3c53`
    FOREIGN KEY (`user_keycloak_id`)
    REFERENCES `user_profiles` (`keycloak_id`),
  CONSTRAINT `FKou33ap52w7r0gpvltur3ypta6`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE UNIQUE INDEX `UKeb9obobu7v2hb1s44vchb9va3` ON `project_favorites` (`user_keycloak_id` ASC, `project_id` ASC) VISIBLE;

SHOW WARNINGS;
CREATE INDEX `FKou33ap52w7r0gpvltur3ypta6` ON `project_favorites` (`project_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_invitations`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_invitations` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_invitations` (
  `id` BINARY(16) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `message` VARCHAR(500) NULL DEFAULT NULL,
  `status` ENUM('ACCEPTED', 'PENDING', 'REJECTED') NOT NULL,
  `invited_user_id` BINARY(16) NOT NULL,
  `project_id` BINARY(16) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FKd5ldsqguh3iqby7rrsdvatrwd`
    FOREIGN KEY (`invited_user_id`)
    REFERENCES `user_profiles` (`keycloak_id`),
  CONSTRAINT `FKhk66j7po8n11yhiagqfvtpn0l`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FKd5ldsqguh3iqby7rrsdvatrwd` ON `project_invitations` (`invited_user_id` ASC) VISIBLE;

SHOW WARNINGS;
CREATE INDEX `FKhk66j7po8n11yhiagqfvtpn0l` ON `project_invitations` (`project_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_join_requests`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_join_requests` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_join_requests` (
  `id` BINARY(16) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `message` VARCHAR(500) NULL DEFAULT NULL,
  `status` ENUM('ACCEPTED', 'PENDING', 'REJECTED') NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `project_id` BINARY(16) NOT NULL,
  `requesting_user_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK7xwq3ldwhewibgx0dwkwxmqvt`
    FOREIGN KEY (`requesting_user_id`)
    REFERENCES `user_profiles` (`keycloak_id`),
  CONSTRAINT `FKdoa85mnsm5iq106ee5mat7caa`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FKdoa85mnsm5iq106ee5mat7caa` ON `project_join_requests` (`project_id` ASC) VISIBLE;

SHOW WARNINGS;
CREATE INDEX `FK7xwq3ldwhewibgx0dwkwxmqvt` ON `project_join_requests` (`requesting_user_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_links`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_links` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_links` (
  `id` BINARY(16) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `label` VARCHAR(100) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `url` VARCHAR(300) NOT NULL,
  `project_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FKmo4lq56vrdc8rw945qp25u0dh`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FKmo4lq56vrdc8rw945qp25u0dh` ON `project_links` (`project_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_members`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_members` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_members` (
  `project_id` BINARY(16) NOT NULL,
  `user_profile_keycloak_id` BINARY(16) NOT NULL,
  CONSTRAINT `FKdki1sp2homqsdcvqm9yrix31g`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`),
  CONSTRAINT `FKm4txxtwu4k4varl8x67ry7t2s`
    FOREIGN KEY (`user_profile_keycloak_id`)
    REFERENCES `user_profiles` (`keycloak_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FKm4txxtwu4k4varl8x67ry7t2s` ON `project_members` (`user_profile_keycloak_id` ASC) VISIBLE;

SHOW WARNINGS;
CREATE INDEX `FKdki1sp2homqsdcvqm9yrix31g` ON `project_members` (`project_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `tags`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tags` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `tags` (
  `name` VARCHAR(30) NOT NULL,
  PRIMARY KEY (`name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_tags`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_tags` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_tags` (
  `project_id` BINARY(16) NOT NULL,
  `tag_name` VARCHAR(30) NOT NULL,
  PRIMARY KEY (`project_id`, `tag_name`),
  CONSTRAINT `FK7vgpvqeln3a88ex8f4oa695cu`
    FOREIGN KEY (`tag_name`)
    REFERENCES `tags` (`name`),
  CONSTRAINT `FKra1vi3p19o2pqtm3c1geaose9`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FK7vgpvqeln3a88ex8f4oa695cu` ON `project_tags` (`tag_name` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `project_views`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project_views` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `project_views` (
  `id` BINARY(16) NOT NULL,
  `viewed_at` DATETIME(6) NOT NULL,
  `project_id` BINARY(16) NOT NULL,
  `user_keycloak_id` BINARY(16) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK4mxp93iv4b7857ii8ismprtpq`
    FOREIGN KEY (`user_keycloak_id`)
    REFERENCES `user_profiles` (`keycloak_id`),
  CONSTRAINT `FK960p7m9hqmhk1eqbdc8riqiou`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FK960p7m9hqmhk1eqbdc8riqiou` ON `project_views` (`project_id` ASC) VISIBLE;

SHOW WARNINGS;
CREATE INDEX `FK4mxp93iv4b7857ii8ismprtpq` ON `project_views` (`user_keycloak_id` ASC) VISIBLE;

SHOW WARNINGS;

-- -----------------------------------------------------
-- Table `user_profile_tags`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `user_profile_tags` ;

SHOW WARNINGS;
CREATE TABLE IF NOT EXISTS `user_profile_tags` (
  `user_profile_keycloak_id` BINARY(16) NOT NULL,
  `tag_name` VARCHAR(30) NOT NULL,
  PRIMARY KEY (`user_profile_keycloak_id`, `tag_name`),
  CONSTRAINT `FK8f8uovfkpm3muvmwvub0ckl19`
    FOREIGN KEY (`user_profile_keycloak_id`)
    REFERENCES `user_profiles` (`keycloak_id`),
  CONSTRAINT `FKk0hohbcaqex7k6jxa1ir8vh72`
    FOREIGN KEY (`tag_name`)
    REFERENCES `tags` (`name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SHOW WARNINGS;
CREATE INDEX `FKk0hohbcaqex7k6jxa1ir8vh72` ON `user_profile_tags` (`tag_name` ASC) VISIBLE;

SHOW WARNINGS;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

