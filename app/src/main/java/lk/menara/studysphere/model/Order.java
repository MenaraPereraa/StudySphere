package lk.menara.studysphere.model;

import com.google.firebase.Timestamp;

import java.util.List;

public class Order {
    private String orderId;
    private String userId;
    private double totalAmount;
    private String status;
    private Timestamp orderDate;
    private Address shippingAddress;
    private Address billingAddress;
    private List<OrderItem> orderItems;

    public Order() {}

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }

    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public Address getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Address billingAddress) { this.billingAddress = billingAddress; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public static class Address {
        private String name;
        private String email;
        private String contact;
        private String address1;
        private String address2;
        private String city;
        private String postcode;

        public Address() {}

        public static AddressBuilder builder() {
            return new AddressBuilder();
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }

        public String getAddress1() { return address1; }
        public void setAddress1(String address1) { this.address1 = address1; }

        public String getAddress2() { return address2; }
        public void setAddress2(String address2) { this.address2 = address2; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getPostcode() { return postcode; }
        public void setPostcode(String postcode) { this.postcode = postcode; }

        public static class AddressBuilder {
            private String name;
            private String email;
            private String contact;
            private String address1;
            private String address2;
            private String city;
            private String postcode;

            public AddressBuilder name(String name) { this.name = name; return this; }
            public AddressBuilder email(String email) { this.email = email; return this; }
            public AddressBuilder contact(String contact) { this.contact = contact; return this; }
            public AddressBuilder address1(String address1) { this.address1 = address1; return this; }
            public AddressBuilder address2(String address2) { this.address2 = address2; return this; }
            public AddressBuilder city(String city) { this.city = city; return this; }
            public AddressBuilder postcode(String postcode) { this.postcode = postcode; return this; }

            public Address build() {
                Address address = new Address();
                address.name = this.name;
                address.email = this.email;
                address.contact = this.contact;
                address.address1 = this.address1;
                address.address2 = this.address2;
                address.city = this.city;
                address.postcode = this.postcode;
                return address;
            }
        }
    }

    public static class OrderItem {
        private String productId;
        private double unitPrice;
        private int quantity;
        private List<Attribute> attributes;

        public OrderItem() {}

        public static OrderItemBuilder builder() {
            return new OrderItemBuilder();
        }

        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public List<Attribute> getAttributes() { return attributes; }
        public void setAttributes(List<Attribute> attributes) { this.attributes = attributes; }

        public static class Attribute {
            private String name;
            private String value;

            public Attribute() {}

            public static AttributeBuilder builder() {
                return new AttributeBuilder();
            }

            // Getters and Setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }

            public static class AttributeBuilder {
                private String name;
                private String value;

                public AttributeBuilder name(String name) { this.name = name; return this; }
                public AttributeBuilder value(String value) { this.value = value; return this; }

                public Attribute build() {
                    Attribute attribute = new Attribute();
                    attribute.name = this.name;
                    attribute.value = this.value;
                    return attribute;
                }
            }
        }

        public static class OrderItemBuilder {
            private String productId;
            private double unitPrice;
            private int quantity;
            private List<Attribute> attributes;

            public OrderItemBuilder productId(String productId) { this.productId = productId; return this; }
            public OrderItemBuilder unitPrice(double unitPrice) { this.unitPrice = unitPrice; return this; }
            public OrderItemBuilder quantity(int quantity) { this.quantity = quantity; return this; }
            public OrderItemBuilder attributes(List<Attribute> attributes) { this.attributes = attributes; return this; }

            public OrderItem build() {
                OrderItem orderItem = new OrderItem();
                orderItem.productId = this.productId;
                orderItem.unitPrice = this.unitPrice;
                orderItem.quantity = this.quantity;
                orderItem.attributes = this.attributes;
                return orderItem;
            }
        }
    }
}
