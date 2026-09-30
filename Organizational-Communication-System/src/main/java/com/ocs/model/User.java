package com.ocs.model;

/** A system user: employee, manager or administrator. */
public class User {
    public int id;
    public String name, username, passwordHash, role, status, department;
    public int failedAttempts;

    public User() { }

    public User(String name, String username, String passwordHash, String role, String status, String department) {
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.department = department;
    }

    public boolean isAdmin() { return "admin".equals(role); }

    /** Managers and administrators may publish announcements, polls, meetings and calendar events (BR-2). */
    public boolean canManage() { return "manager".equals(role) || "admin".equals(role); }
}
