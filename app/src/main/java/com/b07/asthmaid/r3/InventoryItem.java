package com.b07.asthmaid.r3;

public class InventoryItem {

    public String id; // firebase id
    public String name;
    public String type; // controller or rescue
    public String purchaseDate;
    public String expiryDate;
    public int percentLeft;
    public String lastUpdatedBy; // maybe not needed? ask group

    public InventoryItem() { } // firebase constructor

    public InventoryItem(String name, String type,
                         String purchaseDate, String expiryDate,
                         int percentLeft, String lastUpdatedBy) {
        this.name = name;
        this.type = type;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.percentLeft = percentLeft;
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public boolean isLow() {
        return percentLeft <= 20;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.compareTo(todayString()) < 0;
    }

    private String todayString() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }
}
