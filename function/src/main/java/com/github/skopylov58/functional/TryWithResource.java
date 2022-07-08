package com.github.skopylov58.functional;
//package com.skopylov.functional;
//
//import java.io.Closeable;
//import java.util.ArrayList;
//import java.util.List;
//
//public interface TryWithResource<T> extends Try<T>, Closeable {
//  
//    ThreadLocal<List<Closeable>> resources = ThreadLocal.withInitial(ArrayList::new);  
//
//    TryWithResource<T> autoClose();
//
//    static <T> TryWithResource<T> success(T value) {return new Success<>(value);}
//
//    static <T> TryWithResource<T> failure(Exception value) {return new Failure<>(value);}
//    
//    static <T> TryWithResource<T> of(ExceptionalSupplier<T> supplier) {
//        try {
//            return success(supplier.getWithException());
//        } catch (Exception e) {
//            return failure(e);
//        }
//    }
//    
//    default void close() {
//        List<Closeable> list = resources.get();
//        if (list == null || list.isEmpty()) {
//            return;
//        }
//        list.forEach(c -> {
//            try {
//                c.close();
//            } catch (Exception e) {
//                //silently
//            }
//        });
//        list.clear();
//        resources.remove();
//    }
//    
//    class Success<T> extends Try.Success<T> implements TryWithResource<T> {
//
//        Success(T val) {
//            super(val);
//        }
//
//        @Override
//        public TryWithResource<T> autoClose() {
//            if (right instanceof AutoCloseable) {
//                addResource(right);
//            } else {
//                throw new IllegalArgumentException();
//            }
//            return this;
//        }
//
//        private void addResource(T res) {
//            resources.get().add((Closeable)res);
//        }
//    }
//
//    class Failure<T> extends Try.Failure<T> implements TryWithResource<T> {
//
//        Failure(Exception e) {
//            super(e);
//        }
//
//        @Override
//        public TryWithResource<T> autoClose() {
//            return this;
//        }
//    }
//
//}
