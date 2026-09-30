package com.ocs.handler;

import com.ocs.dao.LoginHistoryDao;
import com.ocs.dao.ViolationDao;
import com.ocs.model.LoginRecord;
import com.ocs.model.User;
import com.ocs.model.Violation;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Administrator-only pages: user validation/access, violations, login and audit logs (SC-7). */
public class AdminHandler extends BaseHandler {
    private static final Set<String> ROLES = Set.of("employee", "manager", "admin");
    private static final Set<String> STATUSES = Set.of("active", "suspended", "deactivated");
    private final ViolationDao violations = new ViolationDao();
    private final LoginHistoryDao logs = new LoginHistoryDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        User u = requireRole(ex, "admin");
        if (u == null) return;
        switch (path(ex)) {
            case "/admin/users": users(ex, u); break;
            case "/admin/users/validate": validate(ex, u); break;
            case "/admin/users/status": status(ex, u); break;
            case "/admin/users/role": role(ex, u); break;
            case "/admin/violations": violationList(ex, u); break;
            case "/admin/violations/resolve": resolve(ex, u); break;
            case "/admin/history": history(ex, u); break;
            default: notFound(ex, u);
        }
    }

    private void users(HttpExchange ex, User me) throws Exception {
        StringBuilder b = new StringBuilder("<h1>User Validation &amp; Access</h1>");
        b.append(HtmlUtil.flash(FormParser.query(ex).get("msg"), "done", "Change saved.", "!invalid", "That change is not allowed."));

        b.append("<div class='card'><h2>Pending Registrations</h2>")
                .append("<table><tr><th>Name</th><th>Username</th><th>Role</th><th></th></tr>");
        List<User> pending = USERS.findByStatus("pending");
        for (User x : pending) {
            b.append("<tr><td>").append(HtmlUtil.esc(x.name)).append("</td><td>").append(HtmlUtil.esc(x.username))
                    .append("</td><td>").append(HtmlUtil.esc(x.role)).append("</td><td>")
                    .append(HtmlUtil.postForm("/admin/users/validate", "Approve", "btn-small", "id", "" + x.id, "action", "approve"))
                    .append(" ")
                    .append(HtmlUtil.postForm("/admin/users/validate", "Reject", "btn-small btn-danger", "id", "" + x.id, "action", "reject"))
                    .append("</td></tr>");
        }
        if (pending.isEmpty()) b.append("<tr><td colspan='4'>No pending registrations.</td></tr>");
        b.append("</table></div>");

        b.append("<div class='card'><h2>All Users</h2><table><tr><th>Name</th><th>Username</th><th>Role</th><th>Status</th><th>Actions</th></tr>");
        for (User x : USERS.findAll()) {
            b.append("<tr><td>").append(HtmlUtil.esc(x.name)).append("</td><td>").append(HtmlUtil.esc(x.username))
                    .append("</td><td>").append(HtmlUtil.esc(x.role)).append("</td><td><span class='badge badge-")
                    .append(HtmlUtil.esc(x.status)).append("'>").append(HtmlUtil.esc(x.status)).append("</span></td><td>");
            if (x.id == me.id) {
                b.append("<span class='muted'>(you)</span>");
            } else if (!x.status.equals("pending") && !x.status.equals("rejected")) {
                if (x.status.equals("active")) {
                    b.append(HtmlUtil.postForm("/admin/users/status", "Suspend", "btn-small btn-danger", "id", "" + x.id, "status", "suspended")).append(" ");
                    b.append(HtmlUtil.postForm("/admin/users/status", "Deactivate", "btn-small btn-danger", "id", "" + x.id, "status", "deactivated")).append(" ");
                } else {
                    b.append(HtmlUtil.postForm("/admin/users/status", "Reactivate", "btn-small", "id", "" + x.id, "status", "active")).append(" ");
                }
                b.append("<form method='POST' action='/admin/users/role' class='inline'><input type='hidden' name='id' value='")
                        .append(x.id).append("'><select name='role' style='display:inline;width:auto;'>");
                for (String r : new String[]{"employee", "manager", "admin"}) {
                    b.append("<option value='").append(r).append("'").append(r.equals(x.role) ? " selected" : "").append(">").append(r).append("</option>");
                }
                b.append("</select><button class='btn-small' type='submit'>Set Role</button></form>");
            }
            b.append("</td></tr>");
        }
        b.append("</table></div>");
        sendHtml(ex, 200, HtmlUtil.page("Users", me, b.toString()));
    }

    private void validate(HttpExchange ex, User me) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/admin/users"); return; }
        Map<String, String> f = FormParser.form(ex);
        int id = FormParser.toInt(f.get("id"), -1);
        boolean approve = "approve".equals(f.get("action"));
        User target = USERS.findById(id);
        if (target == null || !target.status.equals("pending")) { redirect(ex, "/admin/users?msg=invalid"); return; }
        USERS.updateStatus(id, approve ? "active" : "rejected");
        logs.logAdminAction(me.id, approve ? "APPROVE_USER" : "REJECT_USER", target.username);
        redirect(ex, "/admin/users?msg=done");
    }

    private void status(HttpExchange ex, User me) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/admin/users"); return; }
        Map<String, String> f = FormParser.form(ex);
        int id = FormParser.toInt(f.get("id"), -1);
        String status = f.getOrDefault("status", "");
        User target = USERS.findById(id);
        if (target == null || id == me.id || !STATUSES.contains(status)) { redirect(ex, "/admin/users?msg=invalid"); return; }
        USERS.updateStatus(id, status);
        logs.logAdminAction(me.id, "SET_STATUS", target.username + " -> " + status);
        redirect(ex, "/admin/users?msg=done");
    }

    private void role(HttpExchange ex, User me) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/admin/users"); return; }
        Map<String, String> f = FormParser.form(ex);
        int id = FormParser.toInt(f.get("id"), -1);
        String role = f.getOrDefault("role", "");
        User target = USERS.findById(id);
        if (target == null || id == me.id || !ROLES.contains(role)) { redirect(ex, "/admin/users?msg=invalid"); return; }
        USERS.updateRole(id, role);
        logs.logAdminAction(me.id, "SET_ROLE", target.username + " -> " + role);
        redirect(ex, "/admin/users?msg=done");
    }

    private void violationList(HttpExchange ex, User me) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Violations</h1><div class='card'><table>")
                .append("<tr><th>Reporter</th><th>Description</th><th>Reported</th><th>Status</th><th></th></tr>");
        List<Violation> list = violations.listAll();
        for (Violation v : list) {
            b.append("<tr><td>").append(HtmlUtil.esc(v.reporter)).append("</td><td>").append(HtmlUtil.esc(v.description))
                    .append("</td><td>").append(HtmlUtil.fmt(v.reportedAt)).append("</td><td><span class='badge badge-")
                    .append(HtmlUtil.esc(v.status)).append("'>").append(HtmlUtil.esc(v.status)).append("</span></td><td>");
            if (v.status.equals("open")) {
                b.append(HtmlUtil.postForm("/admin/violations/resolve", "Mark Resolved", "btn-small", "id", "" + v.id));
            }
            b.append("</td></tr>");
        }
        if (list.isEmpty()) b.append("<tr><td colspan='5'>No reports.</td></tr>");
        b.append("</table></div>");
        sendHtml(ex, 200, HtmlUtil.page("Violations", me, b.toString()));
    }

    private void resolve(HttpExchange ex, User me) throws Exception {
        if (isPost(ex)) {
            int id = FormParser.toInt(FormParser.form(ex).get("id"), -1);
            violations.resolve(id);
            logs.logAdminAction(me.id, "RESOLVE_VIOLATION", "violation #" + id);
        }
        redirect(ex, "/admin/violations");
    }

    private void history(HttpExchange ex, User me) throws Exception {
        StringBuilder b = new StringBuilder("<h1>Login &amp; Audit Logs</h1><div class='card'><h2>All Logins</h2>")
                .append("<table><tr><th>Username</th><th>Time</th><th>IP address</th></tr>");
        for (LoginRecord r : logs.all()) {
            b.append("<tr><td>").append(HtmlUtil.esc(r.username)).append("</td><td>").append(HtmlUtil.fmt(r.loginTime))
                    .append("</td><td>").append(HtmlUtil.esc(r.ipAddress)).append("</td></tr>");
        }
        b.append("</table></div><div class='card'><h2>Administrator Actions</h2>")
                .append("<table><tr><th>Time</th><th>Admin</th><th>Action</th><th>Details</th></tr>");
        for (String[] row : logs.recentAdminActions(200)) {
            b.append("<tr><td>").append(HtmlUtil.esc(row[0])).append("</td><td>").append(HtmlUtil.esc(row[1]))
                    .append("</td><td>").append(HtmlUtil.esc(row[2])).append("</td><td>").append(HtmlUtil.esc(row[3])).append("</td></tr>");
        }
        b.append("</table></div>");
        sendHtml(ex, 200, HtmlUtil.page("Logs", me, b.toString()));
    }
}
