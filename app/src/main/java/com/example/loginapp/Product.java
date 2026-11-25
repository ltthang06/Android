package com.example.loginapp;

public class Product {
    private final String id;
    private final String name;
    private final int price;
    private final int image;
    private final String description;

    public Product(String id, String name, int price, int image, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public String getPriceText() {
        return Money.format(price);
    }
}
