package Assets;

import java.io.Serializable;
import java.time.LocalDate;

public class News implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String newsId;
    private String title;
    private String content;
    private final String authorUsername;
    private LocalDate datePosted;

    public News(String newsId, String title, String content, String authorUsername) {
        this.newsId = newsId;
        this.title = title;
        this.content = content;
        this.authorUsername = authorUsername;
        this.datePosted = LocalDate.now();
    }

    public String getNewsId()         { return newsId; }
    public String getTitle()          { return title; }
    public String getContent()        { return content; }
    public String getAuthorUsername() { return authorUsername; }
    public LocalDate getDatePosted()  { return datePosted; }

    public void setTitle(String title)     { this.title = title; }
    public void setContent(String content) { this.content = content; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof News)) return false;
        return newsId.equals(((News) o).newsId);
    }

    @Override
    public int hashCode() { return newsId.hashCode(); }

    @Override
    public String toString() {
        return String.format("[%s] %s (by %s, %s)", newsId, title, authorUsername, datePosted);
    }
}
