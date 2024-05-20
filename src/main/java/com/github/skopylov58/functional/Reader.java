package com.github.skopylov58.functional;


import java.util.function.Function;

public class Reader<CTX, A> {

    private final Function<CTX, A> runner;

    private Reader(Function<CTX, A> runner) {
        this.runner = runner;
    }

    public static <CTX, A> Reader<CTX, A> of(Function<CTX, A> f) {
        return new Reader<>(f);
    }

    public static <CTX, A> Reader<CTX, A> pure(A a) {
        return new Reader<>(ctx -> a);
    }

    public A apply(CTX ctx) {
        return runner.apply(ctx);
    }

    public <U> Reader<CTX, U> map(Function<? super A, ? extends U> f) {
        return new Reader<>(ctx -> f.apply(apply(ctx)));
    }

    public <U> Reader<CTX, U> flatMap(Function<? super A, Reader<CTX, ? extends U>> f) {
        return new Reader<>(ctx -> f.apply(apply(ctx)).apply(ctx));
    }


}
