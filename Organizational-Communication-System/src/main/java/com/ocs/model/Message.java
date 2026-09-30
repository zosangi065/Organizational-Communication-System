package com.ocs.model;

import java.sql.Timestamp;

/** A message between two users. senderName is already masked to "Anonymous" by the DAO when applicable. */
public class Message {
    public int id, senderId, recipientId;
    public String type, body;
    public boolean anonymous;
    public Timestamp sentAt;
    public String senderName, recipientName;
}
