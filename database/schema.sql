-- 首次部署：mysql -u root -p < database/schema.sql
CREATE DATABASE IF NOT EXISTS lab_agent
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE lab_agent;

-- Flyway 的应用迁移位于 src/main/resources/db/migration/V1__schema.sql。
-- 本文件与该迁移保持相同的表定义，供空 MySQL 实例的首次建库使用。
SOURCE src/main/resources/db/migration/V1__schema.sql;
