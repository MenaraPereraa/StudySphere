package lk.menara.studysphere.model;

import java.util.List;

public class CartItem {

    private String id;
    private String productId;
    private String title;
    private double price;
    private int quantity;
    private String imageUrl;
    private boolean checked;
    private List<Attribute> attributes;

    public CartItem() {}

    public CartItem(String id, String title, double price, int quantity, String imageUrl) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.checked = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // ✅ Checkbox getter and setter
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public List<Attribute> getAttributes() { return attributes; }
    public void setAttributes(List<Attribute> attributes) { this.attributes = attributes; }

    public static class Attribute {
        private String name;
        private String value;

        public Attribute() {}

        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}