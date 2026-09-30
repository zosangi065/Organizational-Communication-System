package com.ocs.handler;

import com.ocs.Database;
import com.ocs.dao.LoginHistoryDao;
import com.ocs.model.User;
import com.ocs.util.FormParser;
import com.ocs.util.HtmlUtil;
import com.ocs.util.PasswordHasher;
import com.ocs.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/** Landing page, registration, login (with lockout) and logout. */
public class AuthHandler extends BaseHandler {
    private final LoginHistoryDao history = new LoginHistoryDao();

    @Override
    protected void route(HttpExchange ex) throws Exception {
        switch (path(ex)) {
            case "/": home(ex); break;
            case "/register": register(ex); break;
            case "/login": login(ex); break;
            case "/logout": logout(ex); break;
            default: notFound(ex, null);
        }
    }

    private void home(HttpExchange ex) throws Exception {
        if (currentUser(ex) != null) { redirect(ex, "/dashboard"); return; }
        String flash = HtmlUtil.flash(FormParser.query(ex).get("msg"),
                "registered", "Registration submitted. An administrator must approve your account before you can log in.",
                "loggedout", "You have been logged out.",
                "!badlogin", "Invalid username or password.",
                "!pending", "Your registration is still awaiting administrator approval.",
                "!rejected", "Your registration was rejected. Please contact an administrator.",
                "!suspended", "Your account is suspended. Please contact an administrator.",
                "!deactivated", "Your account has been deactivated.",
                "!locked", "Your account is locked after too many failed logins. Please contact an administrator.",
                "!exists", "That username is already taken.",
                "!invalid", "Please fill in all fields. Username: 3-30 letters/digits/._-  Password: at least 8 characters.");
        String body = flash + "<div class='row'>"
                + "<div class='card'><h2>Login</h2><form method='POST' action='/login'>"
                + "<label>Username</label><input type='text' name='username' required>"
                + "<label>Password</label><input type='password' name='password' required>"
                + "<button type='submit'>Login</button></form></div>"
                + "<div class='card'><h2>Register</h2><form method='POST' action='/register'>"
                + "<label>Full name</label><input type='text' name='name' required>"
                + "<label>Username</label><input type='text' name='username' required>"
                + "<label>Department</label><input type='text' name='department'>"
                + "<label>Password</label><input type='password' name='password' required>"
                + "<label>Role</label><select name='role'><option value='employee'>Employee</option>"
                + "<option value='manager'>Manager</option></select>"
                + "<button type='submit'>Register</button></form></div></div>";
        sendHtml(ex, 200, HtmlUtil.page("Organizational Communication System", null, body));
    }

    private void register(HttpExchange ex) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/"); return; }
        Map<String, String> f = FormParser.form(ex);
        String name = f.getOrDefault("name", "").trim();
        String username = f.getOrDefault("username", "").trim();
        String department = f.getOrDefault("department", "").trim();
        String password = f.getOrDefault("password", "");
        String role = f.getOrDefault("role", "employee");
        if (!role.equals("manager")) role = "employee";   // admins are never self-registered

        if (name.isEmpty() || !username.matches("[A-Za-z0-9_.-]{3,30}") || password.length() < 8) {
            redirect(ex, "/?msg=invalid"); return;
        }
        if (USERS.usernameExists(username)) { redirect(ex, "/?msg=exists"); return; }
        // BR-1: every new account starts as "pending" until an administrator approves it
        USERS.create(new User(name, username, PasswordHasher.hash(password), role, "pending", department));
        redirect(ex, "/?msg=registered");
    }

    private void login(HttpExchange ex) throws Exception {
        if (!isPost(ex)) { redirect(ex, "/"); return; }
        Map<String, String> f = FormParser.form(ex);
        User u = USERS.findByUsername(f.getOrDefault("username", "").trim());
        String password = f.getOrDefault("password", "");

        if (u == null) { redirect(ex, "/?msg=badlogin"); return; }
        if (u.status.equals("locked")) { redirect(ex, "/?msg=locked"); return; }

        if (!PasswordHasher.verify(password, u.passwordHash)) {
            USERS.recordFailedLogin(u.id, Database.getInt("login.max.attempts", 5));
            redirect(ex, "/?msg=badlogin");
            return;
        }
        if (!u.status.equals("active")) {            // correct password, but account not usable (BR-1, BR-6)
            redirect(ex, "/?msg=" + u.status);
            return;
        }
        USERS.resetFailedLogins(u.id);
        history.record(u.id, clientIp(ex));
        setSessionCookie(ex, SessionManager.create(u.id), false);
        redirect(ex, "/dashboard");
    }

    private void logout(HttpExchange ex) throws Exception {
        SessionManager.destroy(cookie(ex, "session"));
        setSessionCookie(ex, "", true);
        redirect(ex, "/?msg=loggedout");
    }
}
