package Services;

import Models.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/messages.dat";

    /** inbox: toUsername → list of received messages */
    private Map<String, List<Message>> inbox;
    private int nextId;

    public MessageService() {
        inbox = new HashMap<>();
        nextId = 1;
    }

    public static MessageService load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (MessageService) ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load messages.dat, starting fresh: " + e.getMessage());
            }
        }
        return new MessageService();
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save messages.dat: " + e.getMessage());
        }
    }

    public void send(String fromUsername, String toUsername, String subject, String content) {
        String id = String.format("MSG-%04d", nextId++);
        Message msg = new Message(id, fromUsername, toUsername, subject, content);
        inbox.computeIfAbsent(toUsername, k -> new ArrayList<>()).add(msg);
    }

    public List<Message> getInbox(String username) {
        return List.copyOf(inbox.getOrDefault(username, List.of()));
    }

    public boolean markAsRead(String username, String messageId) {
        List<Message> messages = inbox.getOrDefault(username, List.of());
        for (Message m : messages) {
            if (m.getMessageId().equals(messageId)) {
                m.markAsRead();
                return true;
            }
        }
        return false;
    }

    public int unreadCount(String username) {
        return (int) inbox.getOrDefault(username, List.of()).stream()
                .filter(m -> !m.isRead()).count();
    }
}
