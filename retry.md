# Неблокирующий повтор (retry) в Java и проект Loom

## Введение

Повтор (retry) операции является старейшим механизмом обеспечения надежности программного обеспечения. Мы используем повторы при выполнении HTTP запросов, запросов к  базам данных, отсылке электронной почты и проч. и проч.

## Наивный повтор

Если Вы программировали, то наверняка писали процедуру какого либо повтора. Простейший повтор использует цикл с некоторым ожиданием после неудачной попытки. Примерно вот так.

```java
    static <T> T retry(long maxTries, Duration delay, CheckedSupplier<T> supp) {
        for (int i = 0; i < maxTries; i++) {
            try {
                return supp.get();
            } catch (Exception e) {
                if (i < maxTries - 1) { //not last attempt
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); //Propagate interruption 
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("Retry failed after %d retries".formatted(maxTries)); 
    }
```

Вот пример использования повтора для получения соединения к базе данных. Мы делаем 10 попыток с задержкой 100 msec.

```java
    Connection retryConnection() {
        return retry(10, Duration.ofMillis(100), () -> DriverManager.getConnection("jdbc:foo"));
    }
```

Этот код блокирует Ваш поток на одну секунду (точнее 900 milliseconds, мы не ждем после последней попытки) потому что ```Thread.sleep()``` является блокирующей операцией. Попробуем оценить производительность метода в терминах количества потоков (threads) и времени. Предположим нам нужно сделать 12 операций повтора. Нам потребуется 12 потоков чтобы выполнить задуманное за минимальное время 1 сек, 6 потоков выполнят повторы на 2 сек, 4 - за три секунды, один поток - за 12 секунд. А что если нам потребуется выполнить 1000 операций повтора? Для быстрого выполнения потребуется 1000 потоков (нет, только не это) или 16 минут в одном потоке. Как мы видим этот метод плохо масштабируется.

Давайте проверим это на тесте

```java
    public void testNaiveRetry() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(4);       //4 threads
        Duration dur = TryUtils.measure(() -> {
            IntStream.range(0, 12)                                  //12 retries
            .mapToObj(i -> ex.submit(() -> this.retryConnection())) //submit retry
            .toList()                                               //collect all 
            .forEach(f -> Try.of(()->f.get()));                     //wait all finished
        });
        System.out.println(dur);                                    
    }

Output: PT2.723379388S
```
Теоретически: 900 ms * 3 = 2.7 sec, хорошее совпадение

## Неблокирующий повтор

А можно ли делать повторы не блокируя потоки? Можно, если вместо ```Thread.sleep()``` использовать перепланирование потока на некоторый момент в будущем при помощи ```CompletableFuture.delayedExecutor()```. Как это сделать можно посмотреть в моем классе ```Retry.java```. Кстати подобный подход используется в неблокирующем методе ```delay()``` в Kotlin.

Retry.java - компактный класс без внешних зависимостей который может делать неблокирующий асинхронный повтор операции ( [исходный код](https://github.com/skopylov58/java-functional-addons/blob/master/function/src/main/java/com/github/skopylov58/retry/Retry.java), [Javadoc](https://skopylov58.github.io/java-functional-addons/com/github/skopylov58/retry/package-summary.html) ).
Полное описание возможностей ```Retry``` [с примерами можно посмотреть тут](https://github.com/skopylov58/java-functional-addons#retryt---non-blocking-asynchronous-functional-retry-procedure) 


Так можно сделать повтор, который мы уже делали выше.

```java
    CompletableFuture<Connection> retryConnection(Executor ex) {
        return Retry.of(() -> DriverManager.getConnection("jdbc:foo"))
            .withFixedDelay(Duration.ofMillis(100))
            .withExecutor(ex)
            .retry(10);
    }
```

Мы видим что здесь возвращается не ```Connection```, а ```CompletableFuture<Connection>```, что говорит об асинхронной природе этого вызова. Давайте попробуем выполнить 1000 повторов в одном потоке при помощи ```Retry```.

```java
    public void testRetry() throws Exception {
        Executor ex = Executors.newSingleThreadExecutor(); //Один поток
        Duration dur = TryUtils.measure(() -> {      //Это удобная утилита для измерения времен
            IntStream.range(0, 1_000)                //1000 раз
            .mapToObj(i -> this.retryConnection(ex)) //запускаем операцию повтора
            .toList()                                //собираем future в список
            .forEach(f -> Try.of(()->f.get()));      //дожидаемся всех результатов
        });
        System.out.println(dur);                     //печатаем прошедшее время
    }

Output: PT1.065544748S    
```

Как мы видим, ```Retry``` не блокируется и выполняет 1000 повторов в одном потоке чуть-чуть более одной секунды. Ура, мы можем эффективно делать повторы.

## Причем здесь проект Loom?

Проект Loom добавляет в JDK 19 виртуальные потоки (пока в стадии preview). Цель введения виртуальных потоков лучше всего описана в [JEP 425](https://openjdk.org/jeps/425) и я рекомендую это к прочтению.

Возвращаясь к нашей теме повтора операций, коротко скажу что ```Thread.sleep()``` более не является блокирующей операцией будучи выполняемой в виртуальном потоке. Точнее говоря, ```sleep()``` приостановит (suspend) виртуальный поток, давая возможность системному потоку (carrier thread) переключиться на выполнение других виртуальных потоков. После истечения срока сна, виртуальный поток возобновит (resume) свою работу. Давайте запустим наивный алгоритм повтора в виртуальных потоках.

```java
    var dur =TryUtils.measure(() -> {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        IntStream.range(0, 1_000)
        .mapToObj(i -> executor.submit(() -> retryConnection()))
        .toList()
        .forEach(f -> Try.of(() -> f.get()));
    });
    System.out.println(dur);

Output: PT1.010342284S
```

Поразительно, имеем чуть более одной секунды на 1000 повторов, как и при использовании ```Retry```.

Проект Loom принесет кардинальные изменения в экосистему Java.

- Стиль виртуальный поток на запрос (thread per request) масштабируется с почти оптимальным использованием процессорных ядер. Этот стиль становится рекомендуемым.

- Виртуальные потоки не усложняют модель программирования и не требуют изучения новых концепций, автор JEP 425 говорит что скорее нужно будет забыть старые привычки ("it may require unlearning habits developed to cope with today's high cost of threads.")

- Многие стандартные библиотеки модифицированы для совместной работы с виртуальными потоками.Так например, блокирующие операции чтения из сокета станут неблокирующими в виртуальном потоке.

- Реактивный/асинхронный стиль программирования становится практически не нужным.

Нас  ждут интересные времена в ближайшем будущем. Я очень надеюсь что виртуальные потоки станут стандартной частью JDK в очередной LTS версии Java. 

Сергей Копылов  
skopylov@gmail.com  
[Резюме](https://github.com/skopylov58/cv)  
[Смотреть код на Github](https://github.com/skopylov58/)  

