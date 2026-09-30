package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.Announcement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDao {

    public void insert(String title, String body, int createdBy) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO announcements(title,body,created_by) VALUES(?,?,?)")) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setInt(3, createdBy);
            ps.executeUpdate();
        }
    }

    public List<Announcement> listAll() throws SQLException {
        List<Announcement> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT a.id,a.title,a.body,a.posted_at,u.name AS author FROM announcements a "
                             + "LEFT JOIN users u ON u.id=a.created_by ORDER BY a.posted_at DESC, a.id DESC")) {
            while (rs.next()) {
                Announcement a = new Announcement();
                a.id = rs.getInt("id");
                a.title = rs.getString("title");
                a.body = rs.getString("body");
                a.postedAt = rs.getTimestamp("posted_at");
                a.author = rs.getString("author");
                list.add(a);
            }
        }
        return list;
    }
}
