package com.github.skopylov58.functional;

import static com.github.skopylov58.functional.Either.catching;

import java.io.Closeable;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.skopylov58.functional.Try.CheckedConsumer;
import com.github.skopylov58.functional.Try.CheckedFunction;
import com.github.skopylov58.functional.Try.CheckedSupplier;

/**
 * Minimal functional Either implementation.
 * 
 * <p>
 * 
 * It does not have left or right projections, map/flatMap/filter operations work only on 
 * right side of Either and do not have effect for left side. Anyway you can use {@link #swap()} for this purpose.
 *
 * <p>
 * 
 * Either intentionally does not have any getX() methods, use {@link #fold(Function, Function)}, 
 * {@link #optional()} or {@link #stream()} instead.
 * 
 * <p>
 * 
 * Either has a set of {@link #catching(CheckedConsumer)} higher order functions  which will be useful for exception handling.
 * 
 * <pre>
 *       var socket = catching(() -> new Socket("foo", 1234));
 *       try (var c = socket.asCloseable()) {
 *           socket.flatMap(catching(Socket::getOutputStream))
 *           .flatMap(catching(o -> {
 *            o.write(new byte[] {1,2,3});
 *        }));
 *    }
 * }));
 * </pre>
 * 
 * @author skopylov@gmail.com
 *
 * @param <L> left side type
 * @param <R> right side type
 */
@SuppressWarnings("rawtypes")
public sealed interface Either<L, R> permits Either.Left, Either.Right {

    /**
     * Checks if this is left side.
     * @return true is this is left side of Either.
     */
    default boolean isLeft() {
        return fold(__ -> true, __ -> false);
    }
    
    /**
     * Checks if this is right side.
     * @return true is this is right side of Either.
     */
    default boolean isRight() {return !isLeft();}
    
    /**
     * Converts Either to Stream
     * @return one element stream for right, empty stream for left.
     */
    default Stream<R> stream() {
        return fold(__ -> Stream.empty(), Stream::of);
    }

    /**
     * Converts Either to Optional
     * @return Optional for right, Optional empty for left.
     */
    default Optional<R> optional() {
        return fold(__ -> Optional.empty(), Optional::ofNullable);
    }
    
    /**
     * Maps right side of the Either. 
     * @param <T> new right type
     * @param mapper mapper function that maps R type to T type
     * @return new Either
     */
    @SuppressWarnings("unchecked")
    default <T> Either<L, T> map(Function<? super R, ? extends T> mapper) {
        return (Either<L, T>) fold(__ -> this, r -> right(mapper.apply(r)));
    }

    /**
     * Flat maps right side of the Either.
     * @param <T> new right type
     * @param mapper R to {@code Either<L,T>}
     * @return new {@code Either<L,T>}
     */
    @SuppressWarnings("unchecked")
    default <T> Either<L, T> flatMap(Function<? super R, ? extends Either<? extends L, ? extends T>> mapper) {
        return (Either<L, T>) fold(__ -> this, mapper::apply);
    }

    /**
     * Filters right side of Either, does not have effect if it is left side.
     * @param predicate condition to test
     * @param leftMapper will map R to L if predicate return false
     * @return Either
     */
    default Either<L,R> filter(Predicate<? super R> predicate, Function<? super R, ? extends L> leftMapper) {
        return fold(
                left -> this,
                right -> predicate.test(right) ? this : left(leftMapper.apply(right)) 
                );
    }

    @SuppressWarnings("unchecked")
    default Either<L,Optional<R>> filter(Predicate<? super R> predicate) {
        return (Either<L, Optional<R>>) fold(
                left -> this,
                right -> predicate.test(right) ? right(Optional.of(right)) : right(Optional.empty()) 
                );
    }

    /**
     * Swaps Either
     * @return swapped Either
     */
    default Either<R, L> swap() {
        return fold(Either::right, Either::left);
    }

    /**
     * Folds Either to type T
     * @param <T> folded type
     * @param leftMapper maps left to T
     * @param rihgtMapper maps right to T
     * @return value of T type
     */
    <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rihgtMapper);

    /**
     * Produces side effects for Either
     * @param leftConsumer left side effect
     * @param rightConsumer right side effect
     * @return this Either
     */
    default Either<L, R> accept(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
        fold(left -> {
                leftConsumer.accept(left);
                return null; 
            }, 
            right -> {
                rightConsumer.accept(right);
                return null;
            }
            );
        return this;
    }

    /**
     * Factory method to produce right side of Either from R value
     * @param <R> type of right side
     * @param <L> type of left side
     * @param right right value
     * @return {@link Either.Right}
     */
    static <L, R> Either<L, R> right(R right) {return new Right<>(right);}

    /**
     * Factory method to produce left side of Either from L value
     * @param <R> type of right side
     * @param <L> type of left side
     * @param left left value
     * @return {@link Either.Left}
     */
    static <L, R> Either<L, R> left(L left) {return new Left<>(left);}
    
    /**
     * Right side of Either.
     * @author skopylov@gmail.com
     *
     * @param <R> type of right side
     * @param <L> type of left side
     */
    record Right<L, R>(R right) implements Either<L, R> {
        @Override
        public <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rihgtMapper) {
            return rihgtMapper.apply(right);
        }
    }
    
    /**
     * Left side of Either.
     * @author skopylov@gmail.com
     *
     * @param <R> type of right side
     * @param <L> type of left side
     */

    record Left<L, R>(L left) implements Either<L, R> {
        @Override
        public <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rihgtMapper) {
            return leftMapper.apply(left);
        }
    }
    
    /**
     * Factory method to produce {@code Either<Exception,R>} from throwing supplier
     * @param <R> right side type
     * @param supplier throwing supplier of R
     * @return Either
     */
    static <R> Either<Exception, R> catching(CheckedSupplier<? extends R> supplier) {
        try {
            return right(supplier.get());
        } catch (Exception e) {
            return left(e);
        }
    }

    /**
     * Lifts throwing {@code R->T} function to total {@code R->Either<Exception,R>} function.
     * May be useful with flatMap.
     * @param <R> right side type
     * @param <T> left side type
     * @param mapper throwing function
     * @return lifted function
     */
    static <R,T> Function<R, Either<Exception, T>> catching(CheckedFunction<? super R, ? extends T> mapper) {
        return param -> {
            try {
                return right(mapper.apply(param));
            } catch (Exception e) {
                return left(e);
            }
        };
    }

    /**
     * Lifts throwing consumer to total {@code R->Either<Exception,R>} function.
     * @param <R> right side type
     * @param consumer
     * @return lifted function
     */
    static <R> Function<R, Either<Exception, R>> catching(CheckedConsumer<? super R> consumer) {
        return param -> {
            try {
                consumer.accept(param);
                return right(param);
            } catch (Exception e) {
                return left(e);
            }
        };
    }
    
    /**
     * Converts this Either to Closeable which is handy to use in try-with-resources block.
     * @return Closeable
     */
    default Closeable asCloseable() {
        return fold(
                left -> () -> {
                    if (left instanceof Closeable clo) {
                        clo.close();
                    } 
                },
                right -> () -> {
                    if (right instanceof Closeable clo) {
                        clo.close();
                    }
                });
    }
    
}


