package com.kkgare.campustag;
public class Asset {

    private String tag;
    private String name;
    private String location;
    private String condition;

    public Asset(String tag, String name, String location, String condition) {
        this.tag = tag;
        this.name = name;
        this.location = location;
        this.condition = condition;
    }

    // Getters
    public String getTag() { return tag; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getCondition() { return condition; }

    // Setters
    public void setTag(String tag) { this.tag = tag; }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setCondition(String condition) { this.condition = condition; }
}
