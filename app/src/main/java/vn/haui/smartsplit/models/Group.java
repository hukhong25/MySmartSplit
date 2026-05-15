package vn.haui.smartsplit.models;

import java.util.List;

public class Group {
    private String id;
    private String name;
    private List<String> memberIds; // List of User UIDs
    private String adminId;
    private String joinCode; // Unique code to join the group

    public Group() {}

    public Group(String id, String name, List<String> memberIds, String adminId, String joinCode) {
        this.id = id;
        this.name = name;
        this.memberIds = memberIds;
        this.adminId = adminId;
        this.joinCode = joinCode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }
}
