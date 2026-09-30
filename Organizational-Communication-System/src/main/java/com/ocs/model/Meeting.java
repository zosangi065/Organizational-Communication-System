package com.ocs.model;

import java.sql.Timestamp;

public class Meeting {
    public int id, createdById;
    public String title, invitees, createdBy;
    public Timestamp meetingTime;
}
