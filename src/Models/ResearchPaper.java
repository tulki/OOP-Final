package Models;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an academic publication in the university research system.
 *
 * <p>Core fields follow the IEEE metadata schema: title, authors, journal,
 * page count, publication date, DOI, citation count, and research field.
 * Optional fields ({@code abstractText}, {@code keywords}) can be set after
 * construction via setters.
 *
 * <p>Natural ordering ({@link Comparable}) is by citation count descending,
 * so sorted collections and {@code Collections.sort} rank the most-cited
 * papers first by default. Use explicit {@link java.util.Comparator} instances
 * (e.g. by date or pages) where a different order is required.
 *
 * <p>Equality and hashing are based on DOI when present; two papers with the
 * same DOI are considered the same publication regardless of other fields.
 */
public class ResearchPaper implements Serializable, Comparable<ResearchPaper> {
    private static final long serialVersionUID = 1L;

    private String title;
    private List<String> authors;
    private String journal;
    private int pages;
    private LocalDate datePublished;
    private String doi;
    private int citations;
    private String field;
    private String abstractText;
    private String keywords;

    public ResearchPaper(String title, List<String> authors, String journal,
                         int pages, LocalDate datePublished, String doi,
                         int citations, String field) {
        this.title = title;
        this.authors = new ArrayList<>(authors);
        this.journal = journal;
        this.pages = pages;
        this.datePublished = datePublished;
        this.doi = doi;
        this.citations = citations;
        this.field = field;
    }

    public String getTitle() { return title; }
    public List<String> getAuthors() { return authors; }
    public String getJournal() { return journal; }
    public int getPages() { return pages; }
    public LocalDate getDatePublished() { return datePublished; }
    public String getDoi() { return doi; }
    public int getCitations() { return citations; }
    public String getField() { return field; }
    public String getAbstractText() { return abstractText; }
    public String getKeywords() { return keywords; }

    public void setCitations(int citations) { this.citations = citations; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    @Override
    public int compareTo(ResearchPaper other) {
        return Integer.compare(other.citations, this.citations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResearchPaper)) return false;
        ResearchPaper that = (ResearchPaper) o;
        return doi != null && doi.equals(that.doi);
    }

    @Override
    public int hashCode() { return doi != null ? doi.hashCode() : 0; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format(
                "\"%s\" - %s | %s (%s) | Citations: %d | Pages: %d | Field: %s",
                title, authors, journal, datePublished, citations, pages, field));
        if (abstractText != null && !abstractText.isBlank())
            sb.append("\n    Abstract: ").append(abstractText);
        if (keywords != null && !keywords.isBlank())
            sb.append("\n    Keywords: ").append(keywords);
        return sb.toString();
    }
}

