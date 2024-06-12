package com.github.skopylov58.functional.samples;

import java.util.function.Consumer;

public record BookRecord(String isbn, String title, String genre, String author, int published, String description) {

  private BookRecord(Builder builder) {
      this(builder.isbn, builder.title, builder.genre, builder.author, builder.published, builder.description);
  }

  public static class Builder {
      private final String isbn;
      private final String title;
      String genre;
      String author;
      int published;
      String description;

      public Builder(String isbn, String title) {
          this.isbn = isbn;
          this.title = title;
      }

      public Builder configure(Consumer<Builder> b) {
        b.accept(this);
        return this;
      }

      public BookRecord build() {
          return new BookRecord(this);
      }
  }

}