package com.customermanager;

public class Customer {
    private final String name;
    private final String province;

    public Customer(String name, String province) {
        this.name = name;
        this.province = province;
    }

    public String getName() { return name; }

    public String getProvince() { return province; }
}