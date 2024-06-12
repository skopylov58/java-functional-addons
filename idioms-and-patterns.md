# Идиомы и паттерны (Design patterns) в функциональной Java

## Декораторы (Decorators)

## Цепочка ответственности (Chain of Responsibility)

## Кэширование результатов (Memoization)

```java
    static <T, R> Function<T, R> memoize(Function<T, R> func, Map<T, R> cache) {
      return t -> cache.computeIfAbsent(t, func::apply);
    }

    static <T, R> Function<T, R> memoize(Function<T, R> func) {
      return memoize(func, new ConcurrentHashMap<>());
    }

```


## Однократное исполнение (Run once)

## Обработка ошибок (Error handling with Monads)

## Создание объектов (Builder)

[Exploring Joshua Bloch’s Builder design pattern in Java](https://blogs.oracle.com/javamagazine/post/exploring-joshua-blochs-builder-design-pattern-in-java)

```java
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
```

Использование

```java
    var bookRec = new BookRecord.Builder("1234", "foo bar")
        .configure(book -> {
          book.author = "author";
          book.description = "desc";
          })
        .build();
```

## Стратегия (Strategy)

## Наблюдатель (Observer) ???

## Шаблонный метод (Template Method)


