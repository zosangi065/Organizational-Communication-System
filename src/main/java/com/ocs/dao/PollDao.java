package com.ocs.dao;

import com.ocs.Database;
import com.ocs.model.Poll;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PollDao {

    public void create(String question, List<String> options, int createdBy) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int pollId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO polls(question,created_by) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, question);
                    ps.setInt(2, createdBy);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); pollId = k.getInt(1); }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO poll_options(poll_id,label) VALUES(?,?)")) {
                    for (String o : options) {
                        ps.setInt(1, pollId);
                        ps.setString(2, o);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();   // all-or-nothing (SR-3)
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<Poll> listAll(int userId) throws SQLException {
        List<Poll> polls = new ArrayList<>();
        try (Connection c = Database.getConnection()) {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id,question FROM polls ORDER BY id DESC")) {
                while (rs.next()) {
                    Poll p = new Poll();
                    p.id = rs.getInt("id");
                    p.question = rs.getString("question");
                    polls.add(p);
                }
            }
            for (Poll p : polls) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT o.id,o.label,COUNT(v.user_id) AS votes FROM poll_options o "
                                + "LEFT JOIN poll_votes v ON v.option_id=o.id WHERE o.poll_id=? "
                                + "GROUP BY o.id,o.label ORDER BY o.id")) {
                    ps.setInt(1, p.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Poll.Option o = new Poll.Option();
                            o.id = rs.getInt("id");
                            o.label = rs.getString("label");
                            o.votes = rs.getInt("votes");
                            p.options.add(o);
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM poll_votes WHERE poll_id=? AND user_id=?")) {
                    ps.setInt(1, p.id);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) { p.voted = rs.next(); }
                }
            }
        }
        return polls;
    }

    /** Records a vote. Returns false if the option is invalid or the user already voted (BR-5). */
    public boolean vote(int pollId, int userId, int optionId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM poll_options WHERE id=? AND poll_id=?")) {
                ps.setInt(1, optionId);
                ps.setInt(2, pollId);
                try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return false; }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO poll_votes(poll_id,user_id,option_id) VALUES(?,?,?)")) {
                ps.setInt(1, pollId);
                ps.setInt(2, userId);
                ps.setInt(3, optionId);
                ps.executeUpdate();
                return true;
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                return false;   // primary key (poll_id, user_id) already exists
            }
        }
    }
}
