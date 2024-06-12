package com.github.skopylov58.functional.samples;

import java.util.function.Consumer;

public class Person {
  final String name;
  final String address;
  
  private Person(String name, String address) {
    this.name = name;
    this.address = address;
  }
  
  public static Person.Builder builder() {
    return new Person.Builder();
  }
  
  public static class Builder {
    String name = "";
    String address = "";
    
    public Builder configure(Consumer<Builder> c) {
      c.accept(this);
      return this;
    }
    
    
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setAddress(String address) {
      this.address = address;
      return this;
    }
    
    public Person build() {
      return new Person(name, address);
    }
  }
}