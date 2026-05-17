package Models;

import Enums.LessonType;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;

public class Lesson implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String lessonId;
    private LessonType type;
    private String room;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public Lesson(String lessonId, LessonType type, String room,
                  DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.lessonId = lessonId;
        this.type = type;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getLessonId()   { return lessonId; }
    public LessonType getType()   { return type; }
    public String getRoom()       { return room; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime()   { return endTime; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s %s-%s",
                lessonId, type, room, dayOfWeek, startTime, endTime);
    }
}

