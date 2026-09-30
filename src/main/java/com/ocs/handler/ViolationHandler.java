package com.ocs.handler;

import com.ocs.dao.ViolationDao;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

/** Lets any logged-in user report a problem or policy violation for administrator review. */
public class ViolationHandler extends BaseHandler {
    private final ViolationDao violations = new ViolationDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        if (!path(ex).equals("/violations/report")) { notFound(ex, null); return; }
        User u = requireLogin(ex);
        if (u == null) return;

        if (isPost(ex)) {
            String d = FormParser.form(ex).getOrDefault("description", "").trim();
            if (d.isEmpty()) { redirect(ex, "/violations/report?msg=invalid"); return; }
            violations.insert(u.id, d);
            redirect(ex, "/violations/report?msg=sent");
            return;
        }
        String body = "<h1>Report an Issue</h1>"
                + HtmlUtil.flash(FormParser.query(ex).get("msg"),
                "sent", "Your report was submitted to the administrators.",
                "!invalid", "Please describe the issue.")
                + "<div class='card'><form method='POST' action='/violations/report'>"
                + "<label>Description</label><textarea name='description' rows='4' required "
                + "placeholder='Describe the issue or violation'></textarea>"
                + "<button type='submit'>Submit Report</button></form></div>";
        sendHtml(ex, 200, HtmlUtil.page("Report Issue", u, body));
    }
}
