# Обработка исключений в Java в функциональном стиле.

В данной статье автор предоставит информацию о собственной библиотеке для обработки исключений (Exception) в функциональном стиле.

## Предпосылки

В Java начиная с версии 8 появились новые возможности в виде функциональных интерфейсов и потоков (Stream API). Эти возможности позволяют писать код в новом функциональном стиле без явных циклов,временных переменных, условий ветвления и проч. Я уверен что этот стиль программирования станет со временем основным для большинства Java программистов.

Однако применение функционального стиля на практике осложняется тем, что все стандартные функциональные интерфейсы из пакета `java.util.function` не объявляют проверяемых исключений (являются checked exception unaware).

Рассмотрим простой пример преобразования URL из строкового представления к объектам URL.

```java
    public List<URL> urlListTraditional(String[] urls) {
        return Stream.of(urls)
            .map(URL::new)  //MalformedURLException here
            .collect(Collectors.toList());
    }
```

К сожалению данный код не будет компилироваться из-за того, что конструктор URL может выбросить `MalformedURLException`. Правильный код будет выглядеть следующим образом

```java
    public List<URL> urlListTraditional(String[] urls) {
        return Stream.of(urls)
        .map(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException me) {
                return null;
            }
        }).filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
```
Мы должны явно обработать `MalformedURLException`, вернуть `null` из лямбды, а затем отфильтровать нулевые значения в потоке. Увы, такой код на практике нивелирует все преимущества функционального подхода.

При использовании функционального подхода к обработке исключений, код может выглядеть гораздо приятней.

```java     
    public List<URL> urlListWithTry(String[] urls) {
        return Stream.of(urls)
            .map(s -> Try.of(() -> new URL(s)))
            .flatMap(Try::stream)
            .collect(Collectors.toList());
    }
```

Итак, по порядку про `Try<T>`

## Интерфейс `Try<T>`

Интерфейс `Try<T>` представляет собой некоторое вычисление, которое может завершиться успешно с результатом типа T или неуспешно с исключением. `Try<T>` очень похож на Java `Optional<T>`, который может иметь результат типа T или не иметь результата вообще (иметь null значение).

Объекты `Try<T>` создаются с помощью статического фабричного метода `Try.of(...)` который принимает параметром поставщика (supplier) значения типа T, который может выбросить любое исключение Exception.

```java
   Try<URL> url = Try.of(() -> new URL("foo"));
```

Каждый объект `Try<T>` находится в одном из двух состояний - успеха или неудачи, что можно узнать вызывая методы `Try#isSuccess()` или `Try#isFailure()`.

Для логирования исключений подойдет метод `Try#onFailure(Consumer<Exception>)`, для обработки успешных значений - `Try#.onSuccess(Consumer<T>)`.

Многие методы `Try<T>` возвращают также объект Try, что позволяет соединять вызовы методов через точку (method chaining). Вот пример как можно открыть InputStream от строкового представления URL в несколько строк без явного использования `try/catch`.

```
    Optional<InputStream> input =  
        Try.success(urlString)         //Factory method to create success Try from value
        .filter(Objects::nonNull)      //Filter null strings
        .map(URL::new)                 //Creating URL from string, may throw an Exception
        .map(URL::openStream)          //Open URL input stream, , may throw an Exception
        .onFailure(e -> logError(e))   //Log possible error
        .optional();                   //Convert to Java Optional
```

## Интеграция `Try<T>` с Java `Optional<T>` и `Stream<T>`

`Try<T>` легко превращается в `Optional<T>` при помощи метода `Try#optional()`, так что в случае неуспешного Try вернется `Optional.empty`.

Я намеренно сделал `Try<T>` API восьма похожим на Java `Optional<T>` API. Методы `Try#filter(Predicate<T>)` и  `Try#map(Function<T,R>)` имеют аналогичную семантику соответствующих методов из Optional. Так что если Вы знакомы с `Optional<T>`, то Вы легко будете работать с `Try<T>`.

`Try<T>` легко превращается в `Stream<T>` при помощи метода `Try#stream()` точно так же, как это сделано для `Optional#stream()`. Успешный Try превращается в поток (stream) из одного элемента типа T, неуспешный Try - в пустой поток.

Фильтровать успешные попытки в потоке можно двумя способами - первый традиционный с использованием `Try#filter()`

```
    ...
    .filter(Try::isSuccess)
    .map(Try::get)
    ...

```

Второй короче - при помощи `Try#stream()`

```
    ...
    .flatMap(Try::stream)
    ...
```

будет фильтровать в потоке неуспешные попытки и возвращать поток успешных значений.

## Восстановление после сбоев (Recovering from failures)

`Try<T>` имеет встроенные средства `recover(...)` для восстановления после сбоев если вы имеете несколько стратегий для получения результата T. Предположим у Вас есть несколько стратегий:

```
    public T planA();
    public T planB();
    public T planC();

```

Задействовать все три стратегии/плана одновременно в коде можно следующим образом

```
    Try.of(this::planA)
    .recover(this::planB)
    .recover(this::planC)
    .onFailure(...)
    .map(...)
    ...

```

В данном случае сработает только первый успешный план (или ни один из них). Например, если план А не сработал, но сработал план Б, то план С не будет выполняться.


## Работа с ресурсами (`Try<T>` with resources)

`Try<T>` имплементирует AutoCloseable интерфейс, а следовательно `Try<T>` можно использовать внутри `try-with-resource` блока. Допустим нам надо открыть сокет, записать несколько байт в выходной поток сокета и затем закрыть сокет. Соответствующий код с использованием `Try<T>` будет выглядеть следующим образом.

```
    try (var s = Try.of(() -> new Socket("host", 8888))) {
        s.map(Socket::getOutputStream)
        .onSuccess(out -> out.write(new byte[] {1,2,3}))
        .onFailure(e -> System.out.println(e));
    }
```

Сокет будет закрыт при выходе за последнюю фигурную скобку.

## Выводы

`Try<T>` позволяет обрабатывать исключения в функциональном стиле без явного использования конструкций `try/catch/finally` и поможет сделать Ваш код более коротким, выразительным, легким для понимания и сопровождения.

Надеюсь Вы получите удовольствие от использования `Try<T>`

## Ссылки

Автор - Сергей А. Копылов  
e-mail skopylov@gmail.com

Последнее место работы  
Communications Solutions CMS IUM R&D Lab  
Hewlett Packard Enterprise  
Ведущий специалист

[Код библиотеки на github - https://github.com/skopylov58/java-functional-addons](https://github.com/skopylov58/java-functional-addons)

[Try JavaDoc - https://skopylov58.github.io/java-functional-addons/com/skopylov/functional/Try.html](https://skopylov58.github.io/java-functional-addons/com/skopylov/functional/Try.html)

Еще одна функциональная библиотека для Java  - https://www.vavr.io/

