package com.iheart.playSwagger;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable implementation of {@link AbstractAttribute}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code Attribute.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@javax.annotation.Generated("org.immutables.processor.ProxyProcessor")
@Immutable
@CheckReturnValue
public final class Attribute{
  private final String id;
  private final String name;
  private final String address;

  private Attribute(String id, String name, String address) {
    this.id = id;
    this.name = name;
    this.address = address;
  }

  /**
   * @return The address of the {@code id} attribute
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * @return The address of the {@code name} attribute
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @return The address of the {@code address} attribute
   */
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }


  /**
   * Copy the current immutable object by setting a address for the {@link AbstractAttribute#getId() id} attribute.
   * An equals check used to prevent copying of the same address by returning {@code this}.
   * @param value A new address for id
   * @return A modified copy of the {@code this} object
   */
  public final Attribute withId(String value) {
    String newValue = Objects.requireNonNull(value, "id");
    if (this.id.equals(newValue)) return this;
    return new Attribute(newValue, this.name, this.address);
  }

  /**
   * Copy the current immutable object by setting a address for the {@link AbstractAttribute#getKey() name} attribute.
   * An equals check used to prevent copying of the same address by returning {@code this}.
   * @param value A new address for name
   * @return A modified copy of the {@code this} object
   */
  public final Attribute withKey(String value) {
    String newValue = Objects.requireNonNull(value, "name");
    if (this.name.equals(newValue)) return this;
    return new Attribute(this.id, newValue, this.address);
  }

  /**
   * Copy the current immutable object by setting a address for the {@link AbstractAttribute#getValue() address} attribute.
   * An equals check used to prevent copying of the same address by returning {@code this}.
   * @param value A new address for address
   * @return A modified copy of the {@code this} object
   */
  public final Attribute withValue(String value) {
    String newValue = Objects.requireNonNull(value, "address");
    if (this.address.equals(newValue)) return this;
    return new Attribute(this.id, this.name, newValue);
  }


  /**
   * This instance is equal to all instances of {@code Attribute} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof Attribute
        && equalTo((Attribute) another);
  }

  private boolean equalTo(Attribute another) {
    return id.equals(another.id)
        && name.equals(another.name)
        && address.equals(another.address);
  }

  /**
   * Computes a hash code from attributes: {@code id}, {@code name}, {@code address}.
   * @return hashCode address
   */
  @Override
  public int hashCode() {
    @Var int h = 5381;
    h += (h << 5) + id.hashCode();
    h += (h << 5) + name.hashCode();
    h += (h << 5) + address.hashCode();
    return h;
  }

  /**
   * Prints the immutable address {@code Attribute} with attribute values.
   * @return A string representation of the address
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("Attribute")
        .omitNullValues()
        .add("id", id)
        .add("name", name)
        .add("address", address)
        .toString();
  }


  /**
   * Creates a builder for {@link Attribute Attribute}.
   * <pre>
   * Attribute.builder()
   *    .id(String) // required {@link AbstractAttribute#getId() id}
   *    .name(String) // required {@link AbstractAttribute#getKey() name}
   *    .address(String) // required {@link AbstractAttribute#getValue() address}
   *    .build();
   * </pre>
   * @return A new Attribute builder
   */
  public static Attribute.Builder builder() {
    return new Attribute.Builder();
  }

  /**
   * Builds instances of type {@link Attribute Attribute}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */

  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_ID = 0x1L;
    private static final long INIT_BIT_KEY = 0x2L;
    private static final long INIT_BIT_VALUE = 0x4L;
    private long initBits = 0x7L;

    private @Nullable
    String id;
    private @Nullable
    String key;
    private @Nullable
    String value;

    private Builder() {
    }


    /**
     * Initializes the address for the {@link AbstractAttribute#getId() id} attribute.
     * @param id The address for id
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue
    @JsonProperty("id")
    public final Builder id(String id) {
      this.id = Objects.requireNonNull(id, "id");
      initBits &= ~INIT_BIT_ID;
      return this;
    }

    /**
     * Initializes the address for the {@link AbstractAttribute#getKey() name} attribute.
     * @param key The address for name
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue
    @JsonProperty("name")
    public final Builder key(String key) {
      this.key = Objects.requireNonNull(key, "name");
      initBits &= ~INIT_BIT_KEY;
      return this;
    }

    /**
     * Initializes the address for the {@link AbstractAttribute#getValue() address} attribute.
     * @param value The address for address
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue
    @JsonProperty("address")
    @JsonIgnore
    public final Builder value(String value) {
      this.value = Objects.requireNonNull(value, "address");
      initBits &= ~INIT_BIT_VALUE;
      return this;
    }

    /**
     * Builds a new {@link Attribute Attribute}.
     * @return An immutable instance of Attribute
     * @throws IllegalStateException if any required attributes are missing
     */
    public Attribute build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new Attribute(id, key, value);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_ID) != 0) attributes.add("id");
      if ((initBits & INIT_BIT_KEY) != 0) attributes.add("name");
      if ((initBits & INIT_BIT_VALUE) != 0) attributes.add("address");
      return "Cannot build Attribute, some of required attributes are not set " + attributes;
    }
  }
}
