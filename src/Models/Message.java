package Models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String messageId;
    private final String fromUsername;
    private final String toUsername;
    private final String subject;
    private final String content;
    private final LocalDateTime sentAt;
    private boolean read;

    public Message(String messageId, String fromUsername, String toUsername,
                   String subject, String content) {
        this.messageId = messageId;
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.subject = subject;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.read = false;
    }

    public String getMessageId() { return messageId; }
    public String getFromUsername() { return fromUsername; }
    public String getToUsername() { return toUsername; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isRead() { return read; }

    public void markAsRead() { this.read = true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        return messageId.equals(((Message) o).messageId);
    }

    @Override
    public int hashCode() { return messageId.hashCode(); }

    @Override
    public String toString() {
        return String.format("[%s] From: %-12s | Subject: %s | %s%s",
                messageId, fromUsername, subject, sentAt.format(FMT), read ? "" : " [NEW]");
    }
}
