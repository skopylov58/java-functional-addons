package com.github.skopylov58.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Simple functional validator, collects all validation errors. 
 * 
 * @author skopylov@gmail.com
 *
 * @param <T> type to validate
 * @param <E> type of validation error
 */
public class Validation<T, E> {

    record PredicateValidator<T, E>(Predicate<T> predicate, Function<T, E> errorMapper) {}
    
    private final List<PredicateValidator<T,E>> predicateValidators;

    private <R> Validation(List<PredicateValidator<T,E>> errorPredicates) {
        this.predicateValidators = errorPredicates;
    }

    public List<E> validate(T t) {
        List<E> res = new ArrayList<>();
        if (t != null) {
            for (var validator : predicateValidators) {
                if (validator.predicate.test(t)) {
                    res.add(validator.errorMapper.apply(t));
                }
            }
        }
        return res;
    }

    public Either<T, List<E>> validateToEither(T t) {
        List<E> list = validate(t);
        return list.isEmpty() ? Either.right(t) : Either.left(list);
    }
    
    /**
     * Validator builder.
     *
     * @param <T> type to validate
     * @param <E> type of validation error
     */
    public static class Builder<T, E> {
        
        private final List<PredicateValidator<T,E>> validators = new ArrayList<>();
        
        /**
         * Adds new validation to the validator.
         * @param predicate validity predicate, should return true if data is invalid 
         * @param error error to collect if predicate returns true
         * @return this builder
         */
        public Builder<T, E> addValidation(Predicate<T> predicate, E error) {
            validators.add(new PredicateValidator<>(predicate, t -> error));
            return this;
        }

        /**
         * Adds new validation to the validator.
         * @param predicate validity predicate, should return true if data is invalid 
         * @param errorMapper maps invalid data to validation error.
         * @return this builder
         */
        public Builder<T, E> addValidation(Predicate<T> predicate, Function<T, E> errorMapper) {
            validators.add(new PredicateValidator<>(predicate, errorMapper));
            return this;
        }

        /**
         * Builds validator.
         * @return Validation
         */
        public Validation<T, E> build() {
            return new Validation<>(validators);
        }
    }
}
