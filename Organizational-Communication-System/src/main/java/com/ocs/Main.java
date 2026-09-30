package com.ocs;

import com.ocs.dao.UserDao;
import com.ocs.handler.*;
import com.ocs.model.User;
import com.ocs.util.PasswordHasher;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/** Application entry point: wires URL paths to handlers and starts the embedded HTTP server. */
public class Main {

    public static void main(String[] args) throws Exception {
        createFirstAdminIfMissing();

        int port = Database.getInt("server.port", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        AuthHandler auth = new AuthHandler();
        for (String p : new String[]{"/", "/register", "/login", "/logout"}) server.createContext(p, auth);

        DashboardHandler dash = new DashboardHandler();
        server.createContext("/dashboard", dash);
        server.createContext("/history", dash);

        server.createContext("/messages", new MessageHandler());
        server.createContext("/calendar", new CalendarHandler());
        server.createContext("/announcements", new AnnouncementHandler());
        server.createContext("/polls", new PollHandler());
        server.createContext("/meetings", new MeetingHandler());
        server.createContext("/violations", new ViolationHandler());
        server.createContext("/admin", new AdminHandler());

        byte[] css = loadResource("/static/style.css");
        server.createContext("/style.css", ex -> {
            ex.getResponseHeaders().set("Content-Type", "text/css; charset=utf-8");
            ex.sendResponseHeaders(200, css.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(css); }
        });

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("OCS running at http://localhost:" + port);
    }

    /** Creates the initial administrator from config.properties when no admin exists (SRS ID-2). */
    private static void createFirstAdminIfMissing() throws Exception {
        UserDao users = new UserDao();
        if (users.adminExists()) return;
        String username = Database.get("admin.username", "admin");
        String password = Database.get("admin.password", "ChangeMe123!");
        users.create(new User(Database.get("admin.name", "Administrator"), username,
                PasswordHasher.hash(password), "admin", "active", ""));
        System.out.println("First administrator '" + username + "' created. Change the password after first login.");
    }

    private static byte[] loadResource(String path) throws IOException {
        try (InputStream in = Main.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing resource " + path);
            return in.readAllBytes();
        }
    }
}
