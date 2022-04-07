package com.skopylov.functional;

public class Tuple3<F, S, T>  extends Tuple<F, S> {
    
    public final T third;
    
    public Tuple3(F f, S s, T t) {
        super(f ,s);
        third = t;
    }
    
    @Override
    public String toString() {
        String s = third == null ? "null" : third.toString();
        return super.toString() + ", Third=" + s;
    }
    
}
