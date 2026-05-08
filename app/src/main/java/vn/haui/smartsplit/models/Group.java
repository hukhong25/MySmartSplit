package vn.haui.smartsplit.models;

import java.util.List;

public class Group {
    private String id;
    private String name;
    private List<String> memberIds; // List of User UIDs
    private String adminId;

    public Group() {}

    public Group(String id, String name, List<String> memberIds, String adminId) {
        this.id = id;
        this.name = name;
        this.memberIds = memberIds;
        this.adminId = adminId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
}
