package com.ocs.model;

import java.sql.Timestamp;

public class Violation {
    public int id;
    public String reporter, description, status;
    public Timestamp reportedAt;
}
