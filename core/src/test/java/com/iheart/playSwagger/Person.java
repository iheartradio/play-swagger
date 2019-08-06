package com.iheart.playSwagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class Person {
    private Long id;
    private String firstName;
    private Float floatValue;
    private Double doubleValue;
    @JsonProperty(required = true)
    private List<String> customKey;

    private String ignore;

    private LocalDate dayOfBirth;

    private LocalDateTime localDateTime;

    private Instant instant;

    private Integer integer;

    private float aFloat;

    private double aDouble;

    private int anInt;

    private long aLong;

    private Attribute attribute;

    private List<Attribute> attributeList;

    private Set<Attribute> attributeSet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    @JsonIgnore
    public String getIgnore() {
        return ignore;
    }

    public void setIgnore(String ignore) {
        this.ignore = ignore;
    }

    public LocalDate getDayOfBirth() {
        return dayOfBirth;
    }

    public void setDayOfBirth(LocalDate dayOfBirth) {
        this.dayOfBirth = dayOfBirth;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public float getaFloat() {
        return aFloat;
    }

    public void setaFloat(float aFloat) {
        this.aFloat = aFloat;
    }

    public double getaDouble() {
        return aDouble;
    }

    public void setaDouble(double aDouble) {
        this.aDouble = aDouble;
    }

    public int getAnInt() {
        return anInt;
    }

    public void setAnInt(int anInt) {
        this.anInt = anInt;
    }

    public long getaLong() {
        return aLong;
    }

    public void setaLong(long aLong) {
        this.aLong = aLong;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public List<Attribute> getAttributeList() {
        return attributeList;
    }

    public void setAttributeList(List<Attribute> attributeList) {
        this.attributeList = attributeList;
    }

    public Set<Attribute> getAttributeSet() {
        return attributeSet;
    }

    public void setAttributeSet(Set<Attribute> attributeSet) {
        this.attributeSet = attributeSet;
    }
}
