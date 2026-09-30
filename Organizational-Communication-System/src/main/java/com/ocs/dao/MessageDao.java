package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {

    public void insert(Message m) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO messages(sender_id,recipient_id,type,body,anonymous) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, m.senderId);
            ps.setInt(2, m.recipientId);
            ps.setString(3, m.type);
            ps.setString(4, m.body);
            ps.setBoolean(5, m.anonymous);
            ps.executeUpdate();
        }
    }

    /**
     * Inbox for a recipient. For anonymous messages the query never returns the sender's name or id,
     * so the identity cannot leak through the UI (SRS SC-4).
     */
    public List<Message> inbox(int userId) throws SQLException {
        String sql = "SELECT m.id, m.type, m.body, m.anonymous, m.sent_at, "
                + "CASE WHEN m.anonymous THEN 'Anonymous' ELSE u.name END AS sender_name "
                + "FROM messages m JOIN users u ON u.id = m.sender_id "
                + "WHERE m.recipient_id=? ORDER BY m.sent_at DESC, m.id DESC";
        List<Message> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.id = rs.getInt("id");
                    m.type = rs.getString("type");
                    m.body = rs.getString("body");
                    m.anonymous = rs.getBoolean("anonymous");
                    m.sentAt = rs.getTimestamp("sent_at");
                    m.senderName = rs.getString("sender_name");
                    list.add(m);
                }
            }
        }
        return list;
    }

    public List<Message> sent(int userId) throws SQLException {
        String sql = "SELECT m.id, m.type, m.body, m.anonymous, m.sent_at, u.name AS recipient_name "
                + "FROM messages m JOIN users u ON u.id = m.recipient_id "
                + "WHERE m.sender_id=? ORDER BY m.sent_at DESC, m.id DESC";
        List<Message> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.id = rs.getInt("id");
                    m.type = rs.getString("type");
                    m.body = rs.getString("body");
                    m.anonymous = rs.getBoolean("anonymous");
                    m.sentAt = rs.getTimestamp("sent_at");
                    m.recipientName = rs.getString("recipient_name");
                    list.add(m);
                }
            }
        }
        return list;
    }
}
