package com.ocs.util;

import com.ocs.model.User;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/** Small helpers for building the server-rendered HTML pages. */
public final class HtmlUtil {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private HtmlUtil() { }

    /** Escapes text for safe inclusion in HTML (output encoding against script injection, SC-8). */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String fmt(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(TS);
    }

    public static String notice(String text) { return "<div class='notice'>" + esc(text) + "</div>"; }

    public static String error(String text) { return "<div class='error'>" + esc(text) + "</div>"; }

    /**
     * Looks up a flash message by code. spec = code, text, code, text, ...
     * A code starting with '!' is shown as an error, otherwise as a notice.
     */
    public static String flash(String code, String... spec) {
        if (code == null) return "";
        for (int i = 0; i + 1 < spec.length; i += 2) {
            boolean err = spec[i].startsWith("!");
            String c = err ? spec[i].substring(1) : spec[i];
            if (c.equals(code)) return err ? error(spec[i + 1]) : notice(spec[i + 1]);
        }
        return "";
    }

    /** A one-button POST form; kv = name, value, name, value ... (used for every state-changing action). */
    public static String postForm(String action, String label, String cssClass, String... kv) {
        StringBuilder s = new StringBuilder("<form method='POST' action='").append(esc(action)).append("' class='inline'>");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s.append("<input type='hidden' name='").append(esc(kv[i])).append("' value='").append(esc(kv[i + 1])).append("'>");
        }
        return s.append("<button type='submit' class='").append(esc(cssClass)).append("'>")
                .append(esc(label)).append("</button></form>").toString();
    }

    private static void link(StringBuilder n, String href, String text) {
        n.append("<a href='").append(href).append("'>").append(text).append("</a>");
    }

    public static String page(String title, User u, String body) {
        StringBuilder n = new StringBuilder("<div class='nav'><div class='nav-brand'>OrgComm</div><div class='nav-links'>");
        if (u != null) {
            link(n, "/dashboard", "Dashboard");
            link(n, "/messages", "Messages");
            link(n, "/calendar", "Calendar");
            link(n, "/announcements", "Announcements");
            link(n, "/polls", "Polls");
            link(n, "/meetings", "Meetings");
            link(n, "/history", "My Logins");
            if (u.isAdmin()) {
                link(n, "/admin/users", "Users");
                link(n, "/admin/violations", "Violations");
                link(n, "/admin/history", "Logs");
            } else {
                link(n, "/violations/report", "Report Issue");
            }
            n.append("<span class='nav-user'>").append(esc(u.name)).append(" (").append(esc(u.role)).append(")</span>");
            n.append(postForm("/logout", "Logout", "link-btn"));
        }
        n.append("</div></div>");
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + esc(title) + "</title><link rel='stylesheet' href='/style.css'></head><body>"
                + n + "<div class='container'>" + body + "</div></body></html>";
    }
}
