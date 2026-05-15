package vn.haui.smartsplit.models;

import java.util.Map;

public class Expense {
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";

    private String id;
    private String description;
    private double amount;
    private String payerId;
    private String payerName;
    private String groupId;
    private long timestamp;
    private Map<String, Double> splitDetails;
    private String proofImageUrl;
    private String status; // PENDING, COMPLETED, REJECTED
    private boolean isSettlement;

    public Expense() {}

    public Expense(String id, String description, double amount, String payerId, String groupId,
                   long timestamp, Map<String, Double> splitDetails) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.payerId = payerId;
        this.groupId = groupId;
        this.timestamp = timestamp;
        this.splitDetails = splitDetails;
        this.status = STATUS_COMPLETED; // Default for normal expenses
        this.isSettlement = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Map<String, Double> getSplitDetails() { return splitDetails; }
    public void setSplitDetails(Map<String, Double> splitDetails) { this.splitDetails = splitDetails; }
    public String getProofImageUrl() { return proofImageUrl; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSettlement() { return isSettlement; }
    public void setSettlement(boolean settlement) { isSettlement = settlement; }
}
