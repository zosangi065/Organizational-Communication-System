package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String COLS = "id,name,username,password_hash,role,status,department,failed_attempts";

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.id = rs.getInt("id");
        u.name = rs.getString("name");
        u.username = rs.getString("username");
        u.passwordHash = rs.getString("password_hash");
        u.role = rs.getString("role");
        u.status = rs.getString("status");
        u.department = rs.getString("department");
        u.failedAttempts = rs.getInt("failed_attempts");
        return u;
    }

    public User findByUsername(String username) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public User findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    public boolean adminExists() throws SQLException {
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='admin'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public int create(User u) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(name,username,password_hash,role,status,department) VALUES(?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.name);
            ps.setString(2, u.username);
            ps.setString(3, u.passwordHash);
            ps.setString(4, u.role);
            ps.setString(5, u.status);
            ps.setString(6, u.department == null ? "" : u.department);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : -1; }
        }
    }

    public List<User> findAll() throws SQLException {
        return query("SELECT " + COLS + " FROM users ORDER BY id");
    }

    public List<User> findByStatus(String status) throws SQLException {
        List<User> list = new ArrayList<>();
        for (User u : findAll()) if (u.status.equals(status)) list.add(u);
        return list;
    }

    public List<User> findActiveExcept(int id) throws SQLException {
        List<User> list = new ArrayList<>();
        for (User u : findAll()) if (u.status.equals("active") && u.id != id) list.add(u);
        return list;
    }

    private List<User> query(String sql) throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Setting a user back to active also clears the failed-login counter. */
    public void updateStatus(int id, String status) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE users SET status=?, failed_attempts=IF(?='active',0,failed_attempts) WHERE id=?")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateRole(int id, String role) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET role=? WHERE id=?")) {
            ps.setString(1, role);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Counts a failed login and locks the account once the limit is reached (SC-6). */
    public void recordFailedLogin(int id, int maxAttempts) throws SQLException {
        // MySQL evaluates SET assignments left to right, so the IF sees the incremented counter.
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE users SET failed_attempts=failed_attempts+1, "
                             + "status=IF(failed_attempts>=?,'locked',status) WHERE id=? AND status='active'")) {
            ps.setInt(1, maxAttempts);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void resetFailedLogins(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET failed_attempts=0 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
