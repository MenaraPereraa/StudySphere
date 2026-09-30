package lk.menara.studysphere.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id = "";
    private String productId = "";
    private String name = "";
    private String category = "";
    private String description = "";
    private double price = 0.0;
    private float rating = 0.0f;
    private int availableQty = 0;
    private int viewCount = 0;

    private String image = "";

    private List<Review> reviews = new ArrayList<>();

    // 📍 Location
    private double latitude = 0.0;
    private double longitude = 0.0;
    private Map<String, Object> location;

    // 🔥 Discovery timestamp (for sorting by rate of discovery)
    private long createdAt = System.currentTimeMillis();

    private String sellerId = "";

    public Product() {}

    // ===== GETTERS & SETTERS =====

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId != null ? productId : "";
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name != null ? name : "No Name";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category != null ? category : "";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description != null ? description : "No description available";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(int availableQty) {
        this.availableQty = availableQty;
    }

    public String getImage() {
        return image != null ? image : "";
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<Review> getReviews() {
        if (reviews == null) {
            reviews = new ArrayList<>();
        }
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    // ===== LOCATION HANDLING =====

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLocation(Map<String, Object> location) {
        this.location = location;

        if (location != null) {
            try {
                Object lat = location.get("lat");
                Object lng = location.get("lng");

                if (lat instanceof Number) {
                    this.latitude = ((Number) lat).doubleValue();
                }

                if (lng instanceof Number) {
                    this.longitude = ((Number) lng).doubleValue();
                }

            } catch (Exception e) {
                e.printStackTrace();
                this.latitude = 0.0;
                this.longitude = 0.0;
            }
        }
    }

    public Map<String, Object> getLocation() {
        return location;
    }

    // ===== DISCOVERY TIMESTAMP =====

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // ===== SAFE PRICE FORMAT =====

    public String getFormattedPrice() {
        try {
            return String.format("LKR %.2f", price);
        } catch (Exception e) {
            return "LKR 0.00";
        }
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getSellerId() {
        return sellerId != null ? sellerId : "";
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}