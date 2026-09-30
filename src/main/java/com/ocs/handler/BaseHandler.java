package com.ocs.handler;

import com.ocs.Database;
import com.ocs.dao.UserDao;
import com.ocs.model.User;
import com.ocs.util.HtmlUtil;
import com.ocs.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Shared plumbing for all page handlers: error handling, sessions, role checks and responses. */
public abstract class BaseHandler implements HttpHandler {
    protected static final UserDao USERS = new UserDao();

    protected abstract void route(HttpExchange ex) throws Exception;

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            route(ex);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendHtml(ex, 500, HtmlUtil.page("Error", null,
                        HtmlUtil.error("The system encountered a problem. Please try again shortly.")));
            } catch (IOException | IllegalStateException ignored) { }
        } finally {
            ex.close();
        }
    }

    protected static String path(HttpExchange ex) { return ex.getRequestURI().getPath(); }

    protected static boolean isPost(HttpExchange ex) { return "POST".equals(ex.getRequestMethod()); }

    protected static String cookie(HttpExchange ex, String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String h : headers) {
            for (String part : h.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) return kv[1];
            }
        }
        return null;
    }

    protected static String clientIp(HttpExchange ex) {
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }

    protected static void setSessionCookie(HttpExchange ex, String token, boolean clear) {
        String v = "session=" + (clear ? "" : token) + "; Path=/; HttpOnly; SameSite=Lax"
                + (clear ? "; Max-Age=0" : "")
                + (Database.getBool("cookie.secure", false) ? "; Secure" : "");
        ex.getResponseHeaders().add("Set-Cookie", v);
    }

    /** The logged-in, still-active user for this request, or null. */
    protected static User currentUser(HttpExchange ex) throws Exception {
        Integer id = SessionManager.getUserId(cookie(ex, "session"));
        if (id == null) return null;
        User u = USERS.findById(id);
        return (u != null && u.status.equals("active")) ? u : null;
    }

    protected static User requireLogin(HttpExchange ex) throws Exception {
        User u = currentUser(ex);
        if (u == null) redirect(ex, "/");
        return u;
    }

    /** Role-based access control (SC-3): returns the user only if their role is one of the allowed roles. */
    protected static User requireRole(HttpExchange ex, String... roles) throws Exception {
        User u = requireLogin(ex);
        if (u == null) return null;
        for (String r : roles) if (r.equals(u.role)) return u;
        sendHtml(ex, 403, HtmlUtil.page("Forbidden", u, HtmlUtil.error("You do not have permission to view this page.")));
        return null;
    }

    protected static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
    }

    protected static void sendHtml(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    protected static void notFound(HttpExchange ex, User u) throws IOException {
        sendHtml(ex, 404, HtmlUtil.page("Not found", u, "<p>Page not found. <a href='/'>Go home</a></p>"));
    }
}
