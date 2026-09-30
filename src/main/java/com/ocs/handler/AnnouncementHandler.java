package com.ocs.handler;

import com.ocs.dao.AnnouncementDao;
import com.ocs.model.Announcement;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public class AnnouncementHandler extends BaseHandler {
    private final AnnouncementDao announcements = new AnnouncementDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        switch (path(ex)) {
            case "/announcements": {
                User u = requireLogin(ex);
                if (u != null) show(ex, u);
                break;
            }
            case "/announcements/create": {
                User u = requireRole(ex, "manager", "admin");
                if (u != null) create(ex, u);
                break;
            }
            default: notFound(ex, null);
        }
    }

    private void show(HttpExchange ex, User u) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Announcements</h1>");
        b.append(HtmlUtil.flash(FormParser.query(ex).get("msg"), "!invalid", "Title and body are required."));
        if (u.canManage()) {
            b.append("<div class='card'><h2>New Announcement</h2><form method='POST' action='/announcements/create'>")
                    .append("<label>Title</label><input type='text' name='title' required>")
                    .append("<label>Body</label><textarea name='body' rows='3' required></textarea>")
                    .append("<button type='submit'>Post</button></form></div>");
        }
        b.append("<div class='card'><h2>Feed</h2>");
        java.util.List<Announcement> list = announcements.listAll();
        if (list.isEmpty()) b.append("<p>No announcements yet.</p>");
        for (Announcement a : list) {
            b.append("<div class='msg'><b>").append(HtmlUtil.esc(a.title)).append("</b><br>")
                    .append(HtmlUtil.esc(a.body)).append("<br><span class='muted'>")
                    .append(HtmlUtil.esc(a.author == null ? "System" : a.author)).append(" &middot; ")
                    .append(HtmlUtil.fmt(a.postedAt)).append("</span></div>");
        }
        b.append("</div>");
        sendHtml(ex, 200, HtmlUtil.page("Announcements", u, b.toString()));
    }

    private void create(HttpExchange ex, User u) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/announcements"); return; }
        Map<String, String> f = FormParser.form(ex);
        String title = f.getOrDefault("title", "").trim();
        String body = f.getOrDefault("body", "").trim();
        if (title.isEmpty() || body.isEmpty()) { redirect(ex, "/announcements?msg=invalid"); return; }
        announcements.insert(title, body, u.id);
        redirect(ex, "/announcements");
    }
}
