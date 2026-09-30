package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.LoginRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Login history plus the administrator audit trail (SRS SR-5). */
public class LoginHistoryDao {

    public void record(int userId, String ip) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO login_history(user_id,ip_address) VALUES(?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, ip);
            ps.executeUpdate();
        }
    }

    public List<LoginRecord> forUser(int userId) throws SQLException {
        return query("SELECT u.username,h.login_time,h.ip_address FROM login_history h "
                + "JOIN users u ON u.id=h.user_id WHERE h.user_id=? ORDER BY h.login_time DESC, h.id DESC LIMIT 200", userId);
    }

    public List<LoginRecord> all() throws SQLException {
        return query("SELECT u.username,h.login_time,h.ip_address FROM login_history h "
                + "JOIN users u ON u.id=h.user_id ORDER BY h.login_time DESC, h.id DESC LIMIT 500", null);
    }

    private List<LoginRecord> query(String sql, Integer param) throws SQLException {
        List<LoginRecord> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoginRecord r = new LoginRecord();
                    r.username = rs.getString("username");
                    r.loginTime = rs.getTimestamp("login_time");
                    r.ipAddress = rs.getString("ip_address");
                    list.add(r);
                }
            }
        }
        return list;
    }

    public void logAdminAction(int adminId, String action, String details) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO audit_log(admin_id,action,details) VALUES(?,?,?)")) {
            ps.setInt(1, adminId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        }
    }

    /** Each row: time, admin username, action, details. */
    public List<String[]> recentAdminActions(int limit) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT a.created_at,u.username,a.action,a.details FROM audit_log a "
                             + "JOIN users u ON u.id=a.admin_id ORDER BY a.created_at DESC, a.id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{String.valueOf(rs.getTimestamp(1)), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
            }
        }
        return rows;
    }
}
