package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.CalendarEvent;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarDao {

    public void insert(String title, LocalDate date, String description, int createdBy) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO calendar_events(title,event_date,description,created_by) VALUES(?,?,?,?)")) {
            ps.setString(1, title);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, description);
            ps.setInt(4, createdBy);
            ps.executeUpdate();
        }
    }

    public List<CalendarEvent> listAll() throws SQLException {
        List<CalendarEvent> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id,title,event_date,description FROM calendar_events ORDER BY event_date, id")) {
            while (rs.next()) {
                CalendarEvent e = new CalendarEvent();
                e.id = rs.getInt("id");
                e.title = rs.getString("title");
                e.date = rs.getString("event_date");
                e.description = rs.getString("description");
                list.add(e);
            }
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM calendar_events WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
