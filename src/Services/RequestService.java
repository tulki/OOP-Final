package Services;

import Enums.RequestStatus;
import Enums.RequestType;
import Models.Request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RequestService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/requests.dat";

    private List<Request> requests;
    private int nextId;

    public RequestService() {
        requests = new ArrayList<>();
        nextId = 1;
    }

    public static RequestService load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (RequestService) ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load requests.dat, starting fresh: " + e.getMessage());
            }
        }
        return new RequestService();
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save requests.dat: " + e.getMessage());
        }
    }

    public Request submit(String fromUsername, RequestType type, String content) {
        String id = String.format("REQ-%04d", nextId++);
        Request req = new Request(id, fromUsername, type, content);
        requests.add(req);
        return req;
    }

    public List<Request> getAll() {
        return List.copyOf(requests);
    }

    public List<Request> getPending() {
        return requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .toList();
    }

    public List<Request> getByUsername(String username) {
        return requests.stream()
                .filter(r -> r.getFromUsername().equals(username))
                .toList();
    }

    public boolean approve(String requestId, String reviewerUsername, String note) {
        return findById(requestId, reviewerUsername, note, true);
    }

    public boolean reject(String requestId, String reviewerUsername, String note) {
        return findById(requestId, reviewerUsername, note, false);
    }

    private boolean findById(String requestId, String reviewer, String note, boolean approve) {
        for (Request r : requests) {
            if (r.getRequestId().equalsIgnoreCase(requestId)
                    && r.getStatus() == RequestStatus.PENDING) {
                if (approve) r.approve(reviewer, note);
                else r.reject(reviewer, note);
                return true;
            }
        }
        return false;
    }
}
