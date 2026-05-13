package Services;

import Actors.Admin;
import Actors.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/users.dat";

    private List<User> users;

    private UserRepository() {
        users = new ArrayList<>();
    }

    public static UserRepository load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (UserRepository) ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load users.dat, starting fresh: " + e.getMessage());
            }
        }
        UserRepository repo = new UserRepository();
        repo.users.add(new Admin("admin", hashPassword("admin"), "Administrator"));
        repo.save();
        return repo;
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save users.dat: " + e.getMessage());
        }
    }

    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public void addUser(User user) {
        if (findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        users.add(user);
    }

    public boolean removeUser(String username) {
        return users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
    }

    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public static String hashPassword(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
