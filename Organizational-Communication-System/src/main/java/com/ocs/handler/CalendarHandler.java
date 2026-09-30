package com.ocs.handler;

import com.ocs.dao.CalendarDao;
import com.ocs.model.CalendarEvent;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.time.LocalDate;
import java.util.Map;

/** Shared calendar: everyone can view, managers and admins can add/delete (BR-2). */
public class CalendarHandler extends BaseHandler {
    private final CalendarDao calendar = new CalendarDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        switch (path(ex)) {
            case "/calendar": {
                User u = requireLogin(ex);
                if (u != null) show(ex, u);
                break;
            }
            case "/calendar/create": {
                User u = requireRole(ex, "manager", "admin");
                if (u != null) create(ex, u);
                break;
            }
            case "/calendar/delete": {
                User u = requireRole(ex, "manager", "admin");
                if (u != null) delete(ex);
                break;
            }
            default: notFound(ex, null);
        }
    }

    private void show(HttpExchange ex, User u) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Calendar</h1>");
        b.append(HtmlUtil.flash(FormParser.query(ex).get("msg"), "!invalid", "Please enter a title and a valid date."));
        if (u.canManage()) {
            b.append("<div class='card'><h2>Add Event</h2><form method='POST' action='/calendar/create'>")
                    .append("<label>Title</label><input type='text' name='title' required>")
                    .append("<label>Date</label><input type='date' name='date' required>")
                    .append("<label>Description</label><textarea name='description' rows='2'></textarea>")
                    .append("<button type='submit'>Add</button></form></div>");
        }
        b.append("<div class='card'><h2>Upcoming Events</h2><table><tr><th>Date</th><th>Title</th><th>Description</th>");
        if (u.canManage()) b.append("<th></th>");
        b.append("</tr>");
        for (CalendarEvent e : calendar.listAll()) {
            b.append("<tr><td>").append(HtmlUtil.esc(e.date)).append("</td><td>").append(HtmlUtil.esc(e.title))
                    .append("</td><td>").append(HtmlUtil.esc(e.description)).append("</td>");
            if (u.canManage()) {
                b.append("<td>").append(HtmlUtil.postForm("/calendar/delete", "Delete", "btn-small btn-danger",
                        "id", String.valueOf(e.id))).append("</td>");
            }
            b.append("</tr>");
        }
        b.append("</table></div>");
        sendHtml(ex, 200, HtmlUtil.page("Calendar", u, b.toString()));
    }

    private void create(HttpExchange ex, User u) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/calendar"); return; }
        Map<String, String> f = FormParser.form(ex);
        String title = f.getOrDefault("title", "").trim();
        try {
            LocalDate date = LocalDate.parse(f.getOrDefault("date", ""));
            if (title.isEmpty()) throw new IllegalArgumentException();
            calendar.insert(title, date, f.getOrDefault("description", "").trim(), u.id);
            redirect(ex, "/calendar");
        } catch (RuntimeException e) {
            redirect(ex, "/calendar?msg=invalid");
        }
    }

    private void delete(HttpExchange ex) throws Exception {
        if (isPost(ex)) calendar.delete(FormParser.toInt(FormParser.form(ex).get("id"), -1));
        redirect(ex, "/calendar");
    }
}
