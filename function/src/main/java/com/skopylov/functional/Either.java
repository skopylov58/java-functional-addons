package com.skopylov.functional;

/**
 * Minimal functional Either implementation.
 * 
 * @author skopylov@gmail.com
 *
 * @param <R> right side type
 * @param <L> left side type
 */
public interface Either<R, L> {

    /**
     * Checks if this is left side.
     * @return true is this is left side of Either.
     */
    boolean isLeft();
    
    
    /**
     * Checks if this is right side.
     * @return true is this is right side of Either.
     */
    default boolean isRight() {return !isLeft();}
    
    /**
     * Gets right side of either.
     * @return right side or throws {@link IllegalStateException} for the left side.
     * @throws IllegalStateException for left side
     */
    R getRight();
    
    /**
     * Gets left side of either.
     * @return left side or throws {@link IllegalStateException} for the right side.
     * @throws IllegalStateException for right side
     */
    L getLeft();
    
    /**
     * Factory method to produce right side of Either from R value
     * @param <R> type of right side
     * @param <L> type of left side
     * @param right right value
     * @return {@link Either.Right}
     */
    static <R, L> Either<R, L> right(R right) {
        return new Right<>(right);
    }

    /**
     * Factory method to produce left side of Either from L value
     * @param <R> type of right side
     * @param <L> type of left side
     * @param left left value
     * @return {@link Either.Left}
     */
    static <R, L> Either<R, L> left(L left) {
        return new Left<>(left);
    }
    
    /**
     * Right side of Either.
     * @author skopylov@gmail.com
     *
     * @param <R> type of right side
     * @param <L> type of left side
     */
    class Right<R, L> implements Either<R, L> {
        
        protected final R right;
        
        Right (R r ) {right = r;}

        @Override
        public boolean isLeft() {return false;}

        @Override
        public R getRight() {return right;}

        @Override
        public L getLeft() {throw new IllegalStateException("This is right");}

    }
    
    /**
     * Left side of Either.
     * @author skopylov@gmail.com
     *
     * @param <R> type of right side
     * @param <L> type of left side
     */

    class Left<R, L> implements Either<R, L> {
        
        protected final L left;
        
        Left(L l) {left = l;}

        @Override
        public boolean isLeft() {return true;}

        @Override
        public R getRight() {
            throw new IllegalStateException("This is left");
        }

        @Override
        public L getLeft() {
            return left;
        }
        
    }
}


