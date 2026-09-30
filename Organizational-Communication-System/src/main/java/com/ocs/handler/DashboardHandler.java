package com.ocs.handler;

import com.ocs.dao.LoginHistoryDao;
import com.ocs.model.LoginRecord;
import com.ocs.model.User;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;

/** Role-aware dashboard and the personal login history (/history). */
public class DashboardHandler extends BaseHandler {
    private final LoginHistoryDao history = new LoginHistoryDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        User u = requireLogin(ex);
        if (u == null) return;
        if (path(ex).equals("/history")) showHistory(ex, u); else dashboard(ex, u);
    }

    private static String card(String title, String text, String href) {
        return "<div class='card'><h2>" + title + "</h2>" + (text.isEmpty() ? "" : "<p>" + text + "</p>")
                + "<a class='btn' href='" + href + "'>Open</a></div>";
    }

    private void dashboard(HttpExchange ex, User u) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Welcome, " + HtmlUtil.esc(u.name) + "</h1>");
        b.append("<div class='row'>")
                .append(card("Messages", "Send and view messages, complaints, or feedback.", "/messages"))
                .append(card("Calendar", "View company and department events.", "/calendar")).append("</div>");
        b.append("<div class='row'>")
                .append(card("Announcements", "", "/announcements"))
                .append(card("Polls", "", "/polls"))
                .append(card("Meetings", "", "/meetings"))
                .append(card("My Login History", "See your past login activity.", "/history")).append("</div>");
        if (u.isAdmin()) {
            b.append("<div class='row'>")
                    .append(card("User Validation &amp; Access", "", "/admin/users"))
                    .append(card("Violations", "", "/admin/violations"))
                    .append(card("Login &amp; Audit Logs", "", "/admin/history")).append("</div>");
        } else {
            b.append(card("Report an Issue", "", "/violations/report"));
        }
        sendHtml(ex, 200, HtmlUtil.page("Dashboard", u, b.toString()));
    }

    private void showHistory(HttpExchange ex, User u) throws Exception {
        List<LoginRecord> rows = history.forUser(u.id);
        StringBuilder b = new StringBuilder("<h1>My Login History</h1><div class='card'><table><tr><th>Time</th><th>IP address</th></tr>");
        for (LoginRecord r : rows) {
            b.append("<tr><td>").append(HtmlUtil.fmt(r.loginTime)).append("</td><td>")
                    .append(HtmlUtil.esc(r.ipAddress)).append("</td></tr>");
        }
        b.append("</table></div>");
        sendHtml(ex, 200, HtmlUtil.page("Login History", u, b.toString()));
    }
}
