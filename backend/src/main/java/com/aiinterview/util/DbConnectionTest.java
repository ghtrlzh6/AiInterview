package com.aiinterview.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 命令行快速检测远端 MySQL 连通性：mvn -q exec:java -Dexec.mainClass=com.aiinterview.util.DbConnectionTest
 */
public class DbConnectionTest {

    public static void main(String[] args) throws Exception {
        String host = env("DB_HOST", "localhost");
        String user = env("DB_USERNAME", "root");
        String password = env("DB_PASSWORD", "");
        String url = "jdbc:mysql://" + host + ":3306/ai_interview?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";

        System.out.println("Connecting to " + host + ":3306/ai_interview ...");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM t_user");
            rs.next();
            System.out.println("OK - t_user count: " + rs.getInt(1));
            rs = st.executeQuery("SELECT COUNT(*) FROM t_position");
            rs.next();
            System.out.println("OK - t_position count: " + rs.getInt(1));
            System.out.println("Database connection successful!");
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : defaultValue;
    }
}
