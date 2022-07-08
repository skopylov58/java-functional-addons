package com.github.skopylov58.functional;

import java.util.Date;

import com.github.skopylov58.functional.ExceptionalConsumer;

public class TestBase {
    
    
    public static void logException(Exception e) {
        System.out.println(new Date() + " " + e);
    }
    
    ConsumerMock<Exception> exceptionConsumer = new ConsumerMock<>();
    
    public class ConsumerMock<T> implements ExceptionalConsumer<T> {
        private T value;
        @Override
        public void acceptWithException(T t) throws Exception {
            value = t;
            System.out.println("Consumed: " + t.toString());
        }
        void reset() {value = null;}
        boolean handled () {return value != null;}
        
    }
    
    class CloseableMock implements AutoCloseable {
        private final String name;
        boolean closed = false;
        
        CloseableMock(String name) {
            this.name = name;
        }
        @Override
        public void close() throws Exception {
            closed = true;
        }
        @Override
        public String toString() {
            return name + " closed: " + closed;
        }
        boolean isClosed() {return closed;}
    }


}
