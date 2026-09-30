package com.ocs.model;

import java.util.ArrayList;
import java.util.List;

public class Poll {
    public int id;
    public String question;
    public boolean voted;              // has the current user already voted?
    public List<Option> options = new ArrayList<>();

    public static class Option {
        public int id, votes;
        public String label;
    }

    public int total() {
        int t = 0;
        for (Option o : options) t += o.votes;
        return t;
    }
}
