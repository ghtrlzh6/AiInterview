-- 已有库升级：为用户表增加简历分段字段（可重复执行，已存在则跳过）
-- 新库请直接使用 sql/init.sql 建表，无需执行本脚本

USE ai_interview;

SET @db = DATABASE();

-- education_experience
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'education_experience');
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_user ADD COLUMN education_experience TEXT COMMENT ''教育经历（简历提取）'' AFTER major',
    'SELECT ''skip: education_experience'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- personal_skills
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'personal_skills');
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_user ADD COLUMN personal_skills TEXT COMMENT ''个人能力（简历提取）'' AFTER education_experience',
    'SELECT ''skip: personal_skills'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- project_experience
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'project_experience');
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_user ADD COLUMN project_experience TEXT COMMENT ''项目经历（简历提取）'' AFTER personal_skills',
    'SELECT ''skip: project_experience'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- internship_experience
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'internship_experience');
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_user ADD COLUMN internship_experience TEXT COMMENT ''实习/工作经历（简历提取）'' AFTER project_experience',
    'SELECT ''skip: internship_experience'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_user'
  AND COLUMN_NAME IN ('education_experience', 'personal_skills', 'project_experience', 'internship_experience')
ORDER BY ORDINAL_POSITION;
