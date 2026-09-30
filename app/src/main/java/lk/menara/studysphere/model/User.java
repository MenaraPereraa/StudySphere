package lk.menara.studysphere.model;

import java.io.Serializable;

public class User implements Serializable {

    private String uid;
    private String name;
    private String email;
    private String mobile;
    private String profilePicUrl;
    private String role;
    private boolean verified;
    private String status;

    private String address;
    private String city;
    private String postalCode;

    public User() {
        this.role = "buyer";
        this.verified = false;
        this.status = "pending";
    }

    public User(String uid, String name, String email, String mobile, String profilePicUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.profilePicUrl = profilePicUrl;
        this.role = "buyer";
        this.verified = false;
        this.status = "pending";
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}