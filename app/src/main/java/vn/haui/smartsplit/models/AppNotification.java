package vn.haui.smartsplit.models;

import com.google.firebase.firestore.PropertyName;

public class AppNotification {
    private String id;
    private String userId; 
    private String title;
    private String content;
    private long timestamp;
    private String type; 
    private String relatedId;
    private boolean read;

    public AppNotification() {}

    public AppNotification(String id, String userId, String title, String content, String type, String relatedId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.relatedId = relatedId;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    @PropertyName("read")
    public boolean isRead() { return read; }
    @PropertyName("read")
    public void setRead(boolean read) { this.read = read; }
}
