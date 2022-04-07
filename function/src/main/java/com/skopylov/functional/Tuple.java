package com.skopylov.functional;


/**
 * Container for pair (first, second) objects.
 * 
 * @author skopylov@gmail.com
 *
 * @param <F> type of first object
 * @param <S> type of second object
 */
public class Tuple<F, S> {
    
    public final F first;
    public final S second;
    
    /**
     * Constructor.
     * @param first first object
     * @param second second object
     */
    public Tuple(F first, S second) {
        this.first = first;
        this.second = second;
    }
    
    @Override
    public String toString() {
        String f = first == null ? "null" : first.toString();
        String s = second == null ? "null" : second.toString();
        return "First=" + f + ", Second=" + s;  
    }
}
