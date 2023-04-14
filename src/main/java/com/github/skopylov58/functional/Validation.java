package com.github.skopylov58.functional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Simple functional validator collecting all validation errors. 
 * 
 * @author skopylov@gmail.com
 *
 * @param <T> type to validate
 * @param <E> type of validation error
 */
public class Validation<T, E> {

    sealed interface Validator permits PredicateValidator, NestedValidator {}
    
    private record PredicateValidator<T, E>(
            Predicate<T> predicate,
            Function<T, E> errorMapper) implements Validator {}
    
    record NestedValidator<T,R,E>(
            Function<T,R> mapper,
            Validation<R,E> validation) implements Validator {}
    

    private final List<Validator> validators;

    /**
     * Private constructor
     * @param errorPredicates validation predicates
     */
    private Validation(List<Validator> validators) {
        this.validators = validators;
    }
    
    /**
     * Validates data.
     * @param t data to validate, null data will not be validated. 
     * @return list of validation errors, empty list if there are not any errors.
     */
    @SuppressWarnings({ "preview", "unchecked" }) 
    public List<E> validate(T t) {
        if (t == null) {
            return Collections.emptyList();
        }
        List<E> errors = new ArrayList<>();
        for (Validator validator : validators) {
            
            if (validator instanceof PredicateValidator pv) {
              boolean err = pv.predicate.test(t);
              if (err) {
                  errors.add((E) pv.errorMapper.apply(t));
              }
            } else if (validator instanceof NestedValidator nv) {
              errors.addAll(nv.validation.validate(nv.mapper.apply(t)));
            } else {
                throw new IllegalStateException();
            }
        }
        return errors;
    }
    
    

    /**
     * Validates data
     * @param t data to validate
     * @return Either
     */
    public Either<List<E>, T> validateToEither(T t) {
        List<E> list = validate(t);
        return list.isEmpty() ? Either.right(t) : Either.left(list);
    }
    
    /**
     * Creates validation builder.
     * @param <T> type to validate
     * @param <E> validation error type
     * @return validation builder
     */
    public static <T, E> Builder<T, E> builder() {
        return new Builder<>();
    }
    
    /**
     * Validator builder.
     *
     * @param <T> type to validate
     * @param <E> type of validation error
     */
    public static class Builder<T, E> {
        
        private final List<Validator> validators = new ArrayList<>();
        
        private Builder() {}
        
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
         * Add new validation of the R type field.
         * @param <R> mapper result type.
         * @param mapper maps T to R
         * @param validation validation for R
         * @return
         */
        public <R> Builder<T, E> addValidation(Function<T, R> mapper, Validation<R, E> validation) {
            validators.add(new NestedValidator(mapper, validation));
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
