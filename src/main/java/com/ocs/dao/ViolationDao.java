package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.Violation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ViolationDao {

    public void insert(int reporterId, String description) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO violations(reporter_id,description) VALUES(?,?)")) {
            ps.setInt(1, reporterId);
            ps.setString(2, description);
            ps.executeUpdate();
        }
    }

    public List<Violation> listAll() throws SQLException {
        List<Violation> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT v.id,v.description,v.status,v.reported_at,u.username FROM violations v "
                             + "JOIN users u ON u.id=v.reporter_id ORDER BY v.status, v.reported_at DESC")) {
            while (rs.next()) {
                Violation v = new Violation();
                v.id = rs.getInt("id");
                v.description = rs.getString("description");
                v.status = rs.getString("status");
                v.reportedAt = rs.getTimestamp("reported_at");
                v.reporter = rs.getString("username");
                list.add(v);
            }
        }
        return list;
    }

    /** BR-7: a violation stays open until an administrator marks it resolved. */
    public void resolve(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE violations SET status='resolved' WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
