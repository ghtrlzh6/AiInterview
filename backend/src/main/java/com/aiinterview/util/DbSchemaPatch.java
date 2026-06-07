package com.aiinterview.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 对已有库补齐 t_user 简历分段字段。可重复执行。
 * 用法：scripts/apply-db-patch.ps1
 */
public class DbSchemaPatch {

    public static void main(String[] args) throws Exception {
        String host = env("DB_HOST", "localhost");
        String user = env("DB_USERNAME", "root");
        String password = env("DB_PASSWORD", "");
        String url = "jdbc:mysql://" + host + ":3306/ai_interview?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";

        System.out.println("Patching schema on " + host + ":3306/ai_interview ...");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            addColumnIfMissing(conn, "education_experience",
                    "education_experience TEXT COMMENT '教育经历（简历提取）' AFTER major");
            addColumnIfMissing(conn, "personal_skills",
                    "personal_skills TEXT COMMENT '个人能力（简历提取）' AFTER education_experience");
            addColumnIfMissing(conn, "project_experience",
                    "project_experience TEXT COMMENT '项目经历（简历提取）' AFTER personal_skills");
            addColumnIfMissing(conn, "internship_experience",
                    "internship_experience TEXT COMMENT '实习/工作经历（简历提取）' AFTER project_experience");
            printColumns(conn);
            System.out.println("Schema patch completed.");
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void addColumnIfMissing(Connection conn, String column, String ddl) throws Exception {
        if (columnExists(conn, column)) {
            System.out.println("Skip (exists): " + column);
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE t_user ADD COLUMN " + ddl);
            System.out.println("Added column: " + column);
        }
    }

    private static boolean columnExists(Connection conn, String column) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static void printColumns(Connection conn) throws Exception {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' "
                + "AND COLUMN_NAME IN ('education_experience','personal_skills','project_experience','internship_experience') "
                + "ORDER BY ORDINAL_POSITION";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("  - " + rs.getString(1) + " (" + rs.getString(2) + ")");
            }
        }
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : defaultValue;
    }
}
