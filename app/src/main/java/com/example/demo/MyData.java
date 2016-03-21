package com.example.demo;

public class MyData {
    private String DeviceID;
    private String count;
    private String mass;

    public String getDeviceID() {
        return DeviceID;
    }

    public void setDeviceID(String deviceID) {
        DeviceID = deviceID;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getMass() {
        return mass;
    }

    public void setMass(String mass) {
        this.mass = mass;
    }

    public MyData(String deviceID, String mass, String count) {
        DeviceID = deviceID;
        this.mass = mass;
        this.count = count;
    }

    public MyData() {
    }
}