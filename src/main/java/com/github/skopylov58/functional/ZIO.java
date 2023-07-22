package com.github.skopylov58.functional;

import java.util.function.Function;


/**
 * Minimalistic ZIO effects
 * 
 * @param <R> input parameter class
 * @param <E> error class
 * @param <A> result class
 */
public record ZIO<R,E,A>(Function<R, Either<E,A>> run) {
    
    public static <R,E,A> ZIO<R,E,A> succeed(A a) {
        return new ZIO<>(__ -> Either.right(a));
    }

    public static <R,E,A> ZIO<R,E,A> fail(E e) {
        return new ZIO<>(__ -> Either.left(e));
    }

    public <B> ZIO<R,E,B> map(Function<? super A,? extends B> mapper) {
        return new ZIO<>( r -> {
            return run.apply(r).map(mapper);
        });
    }

    public <B> ZIO<R,E,B> flatMap(Function<? super A, ZIO<R,E,B>> mapper) {
        return new ZIO<>(r -> run.apply(r).fold(
                    Either::left,
                    right -> mapper.apply(right).run().apply(r)));
    }

}

