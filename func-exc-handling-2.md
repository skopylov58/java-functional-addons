# Обработка исключений в Java в функциональном стиле. Часть 2.

В [предыдущей статье](https://habr.com/ru/post/676852/) была рассмотрена функциональная обработка исключений с помощью интерфейса `Try<T>`. Статья вызвала определенный интерес читателей и была отмечена в ["Сезоне Java"](https://habr.com/ru/article/673202/).

В данной статье автор продолжит тему и рассмотрит простую и "грамотную" (literate) обработку исключений при помощи функций высшего порядка без использования каких либо внешних зависимостей и сторонних библиотек.

## Решение

Для начала перепишем пример из предыдущей статьи преобразования URL из строкового представления к объектам URL c использованием Optional.

```java
    public List<URL> urlList(String[] urls) {
        return Stream.of(urls)      //Stream<String>
        .map(s -> {
            try {
                return Optional.of(new URL(s));
            } catch (MalformedURLException me) {
                return Optional.empty();
            }
            })                      //Stream<Optional<>URL>
        .flatMap(Optional::stream)  //Stream<URL>, filters empty optionals
        .toList();
    }
```

Общая схема понятна, однако не хотелось бы писать подобный boilerplate код каждый раз когда мы встречается с функцией выбрасывающей проверяемые исключения.

И тут нам помогут следующие функции:

````java
    @FunctionalInterface
    interface CheckedFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Higher-order function to convert partial function T=>R to total function T=>Optional<R>
     * @param <T> function input parameter type
     * @param <R> function result type
     * @param func partial function T=>R that may throw checked exception
     * @return total function T => Optional<R>
     */
    static <T, R> Function<T, Optional<R>> toOptional(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return Optional.ofNullable(func.apply(param));
            } catch (RuntimeException err) {
                throw err;  //All runtime exceptions are treated as errors/bugs
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
````

Дадим некоторые пояснения. `CheckedFunction<T, R>` это функция которая может выбросить исключение при преобразовании T => R.
Подобные функции в терминах функционального программирования называются частичными (partial) функциями, потому что значение функции не определено при некоторых входных значениях параметра.

Функция `toOptional(...)` преобразует частичную (partial) функцию T => R в полную (total) функцию T => Optional<R>. Подобного рода функции, которые принимают параметром и/или возвращают другую функцию, в терминах функционального программирования называются функциями высшего порядка (higher-order function).

С использованием новой функции код примет следующий опрятный вид:

```java
    public List<URL> urlList(String[] urls) {
        return Stream.of(urls)      //Stream<String>
        .map(toOptional(URL::new))  //Stream<Optional<URL>>
        .flatMap(Optional::stream)  //Stream<URL>, filters empty optionals
        .toList();                  //List<URL>
    }
```

И теперь её можно применять везде где возможны проверяемые (checked) исключения.

````java
    List<Number> intList(String [] numbers) {
        NumberFormat format = NumberFormat.getInstance();
        return Stream.of(numbers)       //Stream<String> 
        .map(toOptional(format::parse)) //Checked ParseException may happen here
        .flatMap(Optional::stream)      //Stream<Number>
        .toList();                      //List<Number>
    }
````

## Улучшаем обработку исключений

При использовании `Optional<T>` пропадает информация о самом исключении. В крайнем случае исключение можно залогировать в теле функции `toOptional`, но мы найдем лучшее решение.

Нам нужен любой контейнер, который может содержать значение типа T либо само исключение. В терминах функционального программирования таким контейнером является `Either<Exception, T>`, но к сожаления класса `Either<L,R>` (как и класса `Try<T>`) нет в составе стандартной библиотеки Java.

Вы можете использовать любой подходящий контейнер, которым Вы обладаете. Я же в целях краткости буду использовать следующий 

````java
    //Require Java 14+
    record Result<T>(T result, Exception exception) {
        public boolean failed() {return exception != null;}
        public Stream<T> stream() {return failed() ? Stream.empty() : Stream.of(result);}
    } 
````

Теперь наша функция высшего порядка получит имя `toResult` и будет выглядеть так:

````java
    static <T, R> Function<T, Result<R>> toResult(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return new Result<>(func.apply(param), null);
            } catch (RuntimeException err) {
                throw err;
            } catch (Exception e) {
                return new Result<>(null, e);
            }
        };
    }
````

А вот и применение новой функции toResult()

````java
    List<Number> intListWithResult(String [] numbers) {
        NumberFormat format = NumberFormat.getInstance();
        return Stream.of(numbers)      //Stream<String>
        .map(toResult(format::parse))  //Stream<Result<Number>>, ParseException may happen
        .peek(this::handleErr)         //Stream<Result<Number>>
        .flatMap(Result::stream)       //Stream<Number>
        .toList();                     //List<Number>
    }
    
    void handleErr(Result r) {
        if (r.failed()) {
            System.out.println(r.exception());
        }
    }
````

Теперь возможное проверяемое исключение сохраняется в контейнере Result и его можно обработать в потоке.

## Выводы

Для простой и "грамотной" (literate) обработки проверяемых (checked) исключений в функциональном стиле без использования внешних зависимостей необходимо

1. Выбрать подходящий контейнер для хранения результата. В простейшем случае это может быть `Optional<T>`. Лучше использовать контейнер который может хранить значение результата или перехваченное исключение.

2. Написать функцию высшего порядка которая преобразует частичную функцию `T => R`, которая может выбросить исключение, в полную функцию `T => YourContainer<R>` и применять ее в случае необходимости.

## Ссылки

Автор - Сергей А. Копылов  
e-mail skopylov@gmail.com

[Код на github - https://github.com/skopylov58/java-functional-addons](https://github.com/skopylov58/java-functional-addons)


