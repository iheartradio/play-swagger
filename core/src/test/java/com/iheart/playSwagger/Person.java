package com.iheart.playSwagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Person {
    private Long id;
    private String firstName;
    private Float floatValue;
    private Double doubleValue;
    @JsonProperty(required = true)
    private List<String> customKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Float getFloat() {
        return floatValue;
    }

    public void setFloat(Float floatValue) {
        this.floatValue = floatValue;
    }

    public Double getDouble() {
        return doubleValue;
    }

    public void setDouble(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public List<String> getCustomKey() {
        return customKey;
    }

    public void setCustomKey(List<String> customKey) {
        this.customKey = customKey;
    }
}
