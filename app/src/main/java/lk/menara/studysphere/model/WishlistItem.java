package lk.menara.studysphere.model;

public class WishlistItem {

    private String productId;
    private long addedAt; // timestamp when added to wishlist

    public WishlistItem() {}

    public WishlistItem(String productId) {
        this.productId = productId;
        this.addedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public long getAddedAt() { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
}
