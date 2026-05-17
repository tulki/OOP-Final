package Models;

import Enums.RequestStatus;
import Enums.RequestType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String requestId;
    private final String fromUsername;
    private final RequestType type;
    private final String content;
    private final LocalDateTime createdAt;
    private RequestStatus status;
    private String reviewerUsername;
    private String reviewNote;

    public Request(String requestId, String fromUsername, RequestType type, String content) {
        this.requestId = requestId;
        this.fromUsername = fromUsername;
        this.type = type;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    public String getRequestId() { return requestId; }
    public String getFromUsername() { return fromUsername; }
    public RequestType getType() { return type; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public RequestStatus getStatus() { return status; }
    public String getReviewerUsername() { return reviewerUsername; }
    public String getReviewNote() { return reviewNote; }

    public void approve(String reviewer, String note) {
        this.status = RequestStatus.APPROVED;
        this.reviewerUsername = reviewer;
        this.reviewNote = note;
    }

    public void reject(String reviewer, String note) {
        this.status = RequestStatus.REJECTED;
        this.reviewerUsername = reviewer;
        this.reviewNote = note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;
        return requestId.equals(((Request) o).requestId);
    }

    @Override
    public int hashCode() { return requestId.hashCode(); }

    @Override
    public String toString() {
        String review = (reviewerUsername != null)
                ? " | Reviewed by: " + reviewerUsername + (reviewNote != null ? " — " + reviewNote : "")
                : "";
        return String.format("[%s][%s] From: %-12s | %s | %s%s",
                requestId, type, fromUsername, createdAt.format(FMT), status, review);
    }
}
