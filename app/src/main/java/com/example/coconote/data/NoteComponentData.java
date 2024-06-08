package com.example.coconote.data;

import java.util.List;

public class NoteComponentData {
    private String title;
    private String description;
    private String time;
    private List<String> tags;

    public NoteComponentData(String title, String description, String time, List<String> tags) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public List<String> getTags() {
        return tags;
    }
}

