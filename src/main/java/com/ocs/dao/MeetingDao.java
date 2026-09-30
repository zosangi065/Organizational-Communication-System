package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.Meeting;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MeetingDao {

    /** Splits a comma-separated username list into a trimmed, de-duplicated set. */
    public static Set<String> split(String csv) {
        Set<String> set = new LinkedHashSet<>();
        if (csv == null) return set;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) set.add(t);
        }
        return set;
    }

    public void insert(String title, Timestamp time, String invitees, int createdBy) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO meetings(title,meeting_time,invitees,created_by) VALUES(?,?,?,?)")) {
            ps.setString(1, title);
            ps.setTimestamp(2, time);
            ps.setString(3, invitees);
            ps.setInt(4, createdBy);
            ps.executeUpdate();
        }
    }

    public List<Meeting> listAll() throws SQLException {
        List<Meeting> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT m.id,m.title,m.meeting_time,m.invitees,m.created_by,u.username "
                             + "FROM meetings m JOIN users u ON u.id=m.created_by ORDER BY m.meeting_time")) {
            while (rs.next()) {
                Meeting m = new Meeting();
                m.id = rs.getInt("id");
                m.title = rs.getString("title");
                m.meetingTime = rs.getTimestamp("meeting_time");
                m.invitees = rs.getString("invitees");
                m.createdById = rs.getInt("created_by");
                m.createdBy = rs.getString("username");
                list.add(m);
            }
        }
        return list;
    }

    /**
     * Conflict detection (FR-17): a meeting at the same date/time conflicts if it has the same
     * organiser or shares at least one invitee with the new meeting.
     */
    public boolean hasConflict(Timestamp time, int creatorId, Set<String> invitees) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT invitees, created_by FROM meetings WHERE meeting_time=?")) {
            ps.setTimestamp(1, time);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("created_by") == creatorId) return true;
                    for (String inv : split(rs.getString("invitees"))) {
                        if (invitees.contains(inv)) return true;
                    }
                }
            }
        }
        return false;
    }
}
