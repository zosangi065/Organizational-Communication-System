package com.ocs.handler;

import com.ocs.dao.PollDao;
import com.ocs.model.Poll;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PollHandler extends BaseHandler {
    private final PollDao polls = new PollDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        switch (path(ex)) {
            case "/polls": {
                User u = requireLogin(ex);
                if (u != null) show(ex, u);
                break;
            }
            case "/polls/create": {
                User u = requireRole(ex, "manager", "admin");
                if (u != null) create(ex, u);
                break;
            }
            case "/polls/vote": {
                User u = requireLogin(ex);
                if (u != null) vote(ex, u);
                break;
            }
            default: notFound(ex, null);
        }
    }

    private void show(HttpExchange ex, User u) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Polls</h1>");
        b.append(HtmlUtil.flash(FormParser.query(ex).get("msg"),
                "!invalid", "A poll needs a question and between 2 and 10 options.",
                "!novote", "Your vote could not be recorded (already voted or invalid option)."));
        if (u.canManage()) {
            b.append("<div class='card'><h2>New Poll</h2><form method='POST' action='/polls/create'>")
                    .append("<label>Question</label><input type='text' name='question' required>")
                    .append("<label>Options (comma-separated)</label>")
                    .append("<input type='text' name='options' placeholder='Option A, Option B, Option C' required>")
                    .append("<button type='submit'>Create</button></form></div>");
        }
        List<Poll> list = polls.listAll(u.id);
        for (Poll p : list) {
            b.append("<div class='card'><h2>").append(HtmlUtil.esc(p.question)).append("</h2>");
            if (!p.voted) {
                b.append("<form method='POST' action='/polls/vote'><input type='hidden' name='pollId' value='")
                        .append(p.id).append("'>");
                for (Poll.Option o : p.options) {
                    b.append("<label><input type='radio' name='option' value='").append(o.id)
                            .append("' required style='width:auto;display:inline-block;'> ")
                            .append(HtmlUtil.esc(o.label)).append("</label><br>");
                }
                b.append("<br><button type='submit' class='btn-small'>Vote</button></form>");
            } else {
                b.append("<p><i>You already voted.</i></p>");
            }
            b.append("<table><tr><th>Option</th><th>Votes</th></tr>");
            for (Poll.Option o : p.options) {
                b.append("<tr><td>").append(HtmlUtil.esc(o.label)).append("</td><td>").append(o.votes).append("</td></tr>");
            }
            b.append("</table><p>Total votes: ").append(p.total()).append("</p></div>");
        }
        if (list.isEmpty()) b.append("<div class='card'><p>No polls yet.</p></div>");
        sendHtml(ex, 200, HtmlUtil.page("Polls", u, b.toString()));
    }

    private void create(HttpExchange ex, User u) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/polls"); return; }
        Map<String, String> f = FormParser.form(ex);
        String question = f.getOrDefault("question", "").trim();
        List<String> options = new ArrayList<>();
        for (String o : f.getOrDefault("options", "").split(",")) {
            String t = o.trim();
            if (!t.isEmpty() && !options.contains(t)) options.add(t);
        }
        if (question.isEmpty() || options.size() < 2 || options.size() > 10) {
            redirect(ex, "/polls?msg=invalid");
            return;
        }
        polls.create(question, options, u.id);
        redirect(ex, "/polls");
    }

    private void vote(HttpExchange ex, User u) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/polls"); return; }
        Map<String, String> f = FormParser.form(ex);
        boolean ok = polls.vote(FormParser.toInt(f.get("pollId"), -1), u.id, FormParser.toInt(f.get("option"), -1));
        redirect(ex, ok ? "/polls" : "/polls?msg=novote");
    }
}
