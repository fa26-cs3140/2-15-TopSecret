package org.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Brief {

    private final int id;
    private String title;
    private String date;
    private String text;
    private Integer missionId;

    public Brief(int id, String title, String date, String text, Integer missionId) {

        if (id <= 0) {
            throw new IllegalArgumentException("Brief ID must be positive.");
        }

        this.id = id;

        setTitle(title);
        setDate(date);
        setText(text);
        setMissionId(missionId);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Brief title cannot be null or blank.");
        }

        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        parseDate(date);
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Brief text cannot be null.");
        }

        this.text = text;
    }

    public Integer getMissionId() {
        return missionId;
    }

    public void setMissionId(Integer missionId) {
        if (missionId != null && missionId <= 0) {
            throw new IllegalArgumentException("Mission ID must be positive.");
        }

        this.missionId = missionId;
    }

    private LocalDate parseDate(String date) {
        // Check if date follows the format yyyy-mm-dd
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date must follow yyyy-MM-dd format.");
        }

        try {
            return LocalDate.parse(date);
        }
        catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid calendar date: " + date, e);
        }
    }
}
