package com.ocs.handler;

import com.ocs.dao.MessageDao;
import com.ocs.model.Message;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.Set;

/** Compose, inbox and outbox. Anonymous senders are masked at query level (see MessageDao). */
public class MessageHandler extends BaseHandler {
    private static final Set<String> TYPES = Set.of("feedback", "complaint", "request", "general");
    private final MessageDao messages = new MessageDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        User u = requireLogin(ex);
        if (u == null) return;
        switch (path(ex)) {
            case "/messages": show(ex, u); break;
            case "/messages/send": send(ex, u); break;
            default: notFound(ex, u);
        }
    }

    private void show(HttpExchange ex, User u) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Messages</h1>");
        b.append(HtmlUtil.flash(FormParser.query(ex).get("msg"),
                "sent", "Message sent.",
                "!invalid", "Choose a recipient and type a message before sending."));
        b.append("<div class='card'><h2>Compose</h2><form method='POST' action='/messages/send'>");
        b.append("<label>To</label><select name='to'>");
        for (User o : USERS.findActiveExcept(u.id)) {
            b.append("<option value='").append(o.id).append("'>").append(HtmlUtil.esc(o.name))
                    .append(" (").append(HtmlUtil.esc(o.role)).append(")</option>");
        }
        b.append("</select><label>Type</label><select name='type'>")
                .append("<option value='general'>General message</option><option value='feedback'>Feedback</option>")
                .append("<option value='complaint'>Complaint</option><option value='request'>Request</option></select>");
        b.append("<label>Message</label><textarea name='body' rows='3' required></textarea>");
        b.append("<label><input type='checkbox' name='anonymous' style='width:auto;display:inline-block;'> Send anonymously</label>");
        b.append("<p class='muted'>Anonymity cannot be changed after the message is sent.</p>");
        b.append("<button type='submit'>Send</button></form></div>");

        b.append("<div class='row'><div class='card'><h2>Inbox</h2>");
        java.util.List<Message> inbox = messages.inbox(u.id);
        if (inbox.isEmpty()) b.append("<p>No messages received.</p>");
        for (Message m : inbox) {
            b.append("<div class='msg'><b>").append(HtmlUtil.esc(m.senderName)).append("</b>")
                    .append("<span class='tag'>").append(HtmlUtil.esc(m.type)).append("</span> &mdash; ")
                    .append(HtmlUtil.esc(m.body)).append("<br><span class='muted'>")
                    .append(HtmlUtil.fmt(m.sentAt)).append("</span></div>");
        }
        b.append("</div><div class='card'><h2>Sent</h2>");
        java.util.List<Message> sent = messages.sent(u.id);
        if (sent.isEmpty()) b.append("<p>No messages sent.</p>");
        for (Message m : sent) {
            b.append("<div class='msg'>To <b>").append(HtmlUtil.esc(m.recipientName)).append("</b>")
                    .append(m.anonymous ? " (sent anonymously)" : "")
                    .append("<span class='tag'>").append(HtmlUtil.esc(m.type)).append("</span> &mdash; ")
                    .append(HtmlUtil.esc(m.body)).append("<br><span class='muted'>")
                    .append(HtmlUtil.fmt(m.sentAt)).append("</span></div>");
        }
        b.append("</div></div>");
        sendHtml(ex, 200, HtmlUtil.page("Messages", u, b.toString()));
    }

    private void send(HttpExchange ex, User u) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/messages"); return; }
        Map<String, String> f = FormParser.form(ex);
        int to = FormParser.toInt(f.get("to"), -1);
        String body = f.getOrDefault("body", "").trim();
        String type = f.getOrDefault("type", "general");
        User recipient = USERS.findById(to);

        if (recipient == null || !recipient.status.equals("active") || recipient.id == u.id
                || body.isEmpty() || !TYPES.contains(type)) {       // FR-7: reject incomplete messages
            redirect(ex, "/messages?msg=invalid");
            return;
        }
        Message m = new Message();
        m.senderId = u.id;
        m.recipientId = recipient.id;
        m.type = type;
        m.body = body;
        m.anonymous = f.containsKey("anonymous");
        messages.insert(m);
        redirect(ex, "/messages?msg=sent");
    }
}
