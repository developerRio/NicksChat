package com.originalstocksllc.himanshuraj.nicktest;

public class Users {

    public String name;
    public String image;
    public String status;
    public String image_thumb;
    public String email;

    public Users() {
    }

    public Users(String name, String image, String status, String image_thumb, String email) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.image_thumb = image_thumb;
        this.email = email;
    }

    public String getImage_thumb() {
        return image_thumb;
    }

    public void setImage_thumb(String image_thumb) {
        this.image_thumb = image_thumb;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
