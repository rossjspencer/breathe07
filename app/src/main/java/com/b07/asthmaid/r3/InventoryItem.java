package com.b07.asthmaid.r3;

public class InventoryItem {

    public String id; // firebase id
    public String name;
    public String type; // controller or rescue
    public String purchaseDate;
    public String expiryDate;
    public int percentLeft;
    public int doseCapacity;
    public int remainingDoses; // set to doseCapacity if not explicitly given

    public InventoryItem() { } // firebase constructor

    public InventoryItem(String name, String type,
                         String purchaseDate, String expiryDate,
                         int doseCapacity, int remainingDoses) {
        this.name = name;
        this.type = type;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.doseCapacity = doseCapacity;
        this.remainingDoses = remainingDoses;
        
        updatePercentLeft();
    }
    
    public void updatePercentLeft() {
        if (doseCapacity > 0) {
            this.percentLeft = (int) (((double) remainingDoses / doseCapacity) * 100);
        } else {
            this.percentLeft = 0;
        }
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
