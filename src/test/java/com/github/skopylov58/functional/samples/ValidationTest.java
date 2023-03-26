package com.github.skopylov58.functional.samples;

import org.junit.Test;

import com.github.skopylov58.functional.Validation;

public class ValidationTest {
    
    record Person(String name, int age, String email, Address address) {
        Person() { this(null, 0, null, null); }
        Person(String name) { this(name, 0, null, null); }
        Person(String name, int age) { this(name, age, null, null); }
        Person(String name, int age, String email) { this(name, age, email, null); }
    }
    
    record Address(String country, String city, int zipCode) {}
    
    enum PersonErrors {
        TOO_YOUNG,
        MISSING_NAME,
        NAME_TO_LONG
    }
    
    sealed interface IPersonErrors permits TooYoung, TooLongName, MissingName {}
    record TooYoung(int age) implements IPersonErrors{}
    record TooLongName(String name) implements IPersonErrors{}
    record MissingName() implements IPersonErrors {}
    
    @Test
    public void testStrErr() throws Exception {
        
        var validation = new Validation.Builder<Person, String>()
        .addValidation(p -> p.age < 18, "Too young")
        .addValidation(p -> p.name == null, "Missing name")
        .addValidation(p -> (p.name != null) && (p.name.length() > 16), "Too long name")
        .addValidation(p -> p.email == null, "Missing e-mail")
        .build();

        validation.validate(new Person(null))
        .forEach(System.out::println);
        
        validation.validate(new Person("Very very long name to be checked ", 30))
        .forEach(System.out::println);
    }

    @Test
    public void testEnumErr() throws Exception {
        var validation = new Validation.Builder<Person, PersonErrors>()
        .addValidation(p -> p.age < 18, PersonErrors.TOO_YOUNG)
        .addValidation(p -> p.name == null, PersonErrors.MISSING_NAME)
        .addValidation(p -> (p.name != null) && (p.name.length() > 16), PersonErrors.NAME_TO_LONG)
        .build();
        
        validation.validate(new Person(null, 0, "foo@bar"))
        .forEach(System.out::println);
        
        validation.validate(new Person("Very very long name to be checked", 30, "foo@bar"))
        .forEach(System.out::println);
    }

    @Test
    public void testSealedErr() throws Exception {
         var validation = new Validation.Builder<Person, IPersonErrors>()
        .addValidation(p -> p.age < 18, p -> new TooYoung(p.age))
        .addValidation(p -> p.name == null, new MissingName())
        .addValidation(p -> (p.name != null) && (p.name.length() > 16), p -> new TooLongName(p.name))
        .build();
        
        validation.validate(new Person(null, 15, "foo@bar"))
        .forEach(System.out::println);
        
        validation.validate(new Person("Very very long name to be checked ", 30, "foo@bar"))
        .forEach(System.out::println);
    }

    
}
