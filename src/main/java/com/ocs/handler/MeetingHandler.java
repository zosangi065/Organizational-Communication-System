package com.ocs.handler;

import com.ocs.dao.MeetingDao;
import com.ocs.model.Meeting;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/** Meeting / appointment scheduling with conflict detection (FR-16, FR-17). */
public class MeetingHandler extends BaseHandler {
    private final MeetingDao meetings = new MeetingDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        switch (path(ex)) {
            case "/meetings": {
                User u = requireLogin(ex);
                if (u != null) show(ex, u);
                break;
            }
            case "/meetings/create": {
                User u = requireRole(ex, "manager", "admin");
                if (u != null) create(ex, u);
                break;
            }
            default: notFound(ex, null);
        }
    }

    private void show(HttpExchange ex, User u) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Meetings</h1>");
        b.append(HtmlUtil.flash(FormParser.query(ex).get("msg"),
                "scheduled", "Meeting scheduled.",
                "!conflict", "That time conflicts with an existing meeting for you or one of the invitees.",
                "!invitee", "One or more invitees are not active users. Use their usernames, separated by commas.",
                "!invalid", "Please provide a title and a valid date and time."));
        if (u.canManage()) {
            b.append("<div class='card'><h2>Schedule Meeting</h2><form method='POST' action='/meetings/create'>")
                    .append("<label>Title</label><input type='text' name='title' required>")
                    .append("<label>Date &amp; Time</label><input type='datetime-local' name='datetime' required>")
                    .append("<label>Invitees (comma-separated usernames)</label>")
                    .append("<input type='text' name='invitees' placeholder='jdoe, asmith'>")
                    .append("<button type='submit'>Schedule</button></form></div>");
        }
        b.append("<div class='card'><h2>Scheduled</h2><table><tr><th>When</th><th>Title</th><th>Organiser</th><th>Invitees</th></tr>");
        for (Meeting m : meetings.listAll()) {
            boolean visible = u.isAdmin() || m.createdById == u.id || MeetingDao.split(m.invitees).contains(u.username);
            if (!visible) continue;
            b.append("<tr><td>").append(HtmlUtil.fmt(m.meetingTime)).append("</td><td>").append(HtmlUtil.esc(m.title))
                    .append("</td><td>").append(HtmlUtil.esc(m.createdBy)).append("</td><td>")
                    .append(HtmlUtil.esc(m.invitees)).append("</td></tr>");
        }
        b.append("</table></div>");
        sendHtml(ex, 200, HtmlUtil.page("Meetings", u, b.toString()));
    }

    private void create(HttpExchange ex, User u) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/meetings"); return; }
        Map<String, String> f = FormParser.form(ex);
        String title = f.getOrDefault("title", "").trim();
        Timestamp time;
        try {
            time = Timestamp.valueOf(LocalDateTime.parse(f.getOrDefault("datetime", "")));
        } catch (RuntimeException e) {
            redirect(ex, "/meetings?msg=invalid");
            return;
        }
        if (title.isEmpty()) { redirect(ex, "/meetings?msg=invalid"); return; }

        Set<String> invitees = MeetingDao.split(f.getOrDefault("invitees", ""));
        for (String name : invitees) {
            User inv = USERS.findByUsername(name);
            if (inv == null || !inv.status.equals("active")) { redirect(ex, "/meetings?msg=invitee"); return; }
        }
        if (meetings.hasConflict(time, u.id, invitees)) { redirect(ex, "/meetings?msg=conflict"); return; }

        meetings.insert(title, time, String.join(", ", invitees), u.id);
        redirect(ex, "/meetings?msg=scheduled");
    }
}
