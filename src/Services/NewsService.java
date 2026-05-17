package Services;

import Models.News;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NewsService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/news.dat";

    private List<News> newsList;
    private int nextId;

    public NewsService() {
        newsList = new ArrayList<>();
        nextId = 1;
    }

    public static NewsService load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (NewsService) ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load news.dat, starting fresh: " + e.getMessage());
            }
        }
        return new NewsService();
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save news.dat: " + e.getMessage());
        }
    }

    public News create(String title, String content, String authorUsername) {
        News n = new News("NEWS-" + nextId++, title, content, authorUsername);
        newsList.add(n);
        return n;
    }

    public boolean modify(String newsId, String newTitle, String newContent) {
        for (News n : newsList) {
            if (n.getNewsId().equals(newsId)) {
                if (newTitle  != null) n.setTitle(newTitle);
                if (newContent != null) n.setContent(newContent);
                return true;
            }
        }
        return false;
    }

    public boolean delete(String newsId) {
        return newsList.removeIf(n -> n.getNewsId().equals(newsId));
    }

    public List<News> getAll() { return new ArrayList<>(newsList); }
}

