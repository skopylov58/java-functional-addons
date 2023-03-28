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
    
    enum PersonError {
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
        
        var addressValidation = Validation.<Address, String>builder()
        .addValidation(a -> a.city == null, "Missing city")
        .addValidation(a -> a.zipCode <= 0, "Invalid zip code")
        .build();
        
        
        var personValidation = Validation.<Person, String>builder()
        .addValidation(p -> p.age < 18, "Too young")
        .addValidation(p -> p.name == null, "Missing name")
        .addValidation(p -> (p.name != null) && (p.name.length() > 16), "Too long name")
        .addValidation(p -> p.email == null, "Missing e-mail")
        .addValidation(Person::address, addressValidation)
        .build();

        personValidation.validate(new Person(null))
        .forEach(System.out::println);
        
        personValidation.validate(new Person("Very very long name to be checked ", 30))
        .forEach(System.out::println);
    }

    @Test
    public void testEnumErr() throws Exception {
        var validation = Validation.<Person, PersonError>builder()
        .addValidation(p -> p.age < 18, PersonError.TOO_YOUNG)
        .addValidation(p -> p.name == null, PersonError.MISSING_NAME)
        .addValidation(p -> (p.name != null) && (p.name.length() > 16), PersonError.NAME_TO_LONG)
        .build();
        
        validation.validate(new Person(null, 0, "foo@bar"))
        .forEach(System.out::println);
        
        validation.validate(new Person("Very very long name to be checked", 30, "foo@bar"))
        .forEach(System.out::println);
    }

    @Test
    public void testSealedErr() throws Exception {
         var validation = Validation.<Person, IPersonErrors>builder()
        .addValidation(p -> p.age < 18, p -> new TooYoung(p.age))
        .addValidation(p -> p.name == null, new MissingName())
        .addValidation(p -> (p.name != null) && (p.name.length() > 16), p -> new TooLongName(p.name))
        .build();
        
        validation.validate(new Person(null, 15, "foo@bar"))
        .forEach(System.out::println);
        
        validation.validate(new Person("Very very long name to be checked ", 30, "foo@bar"))
        .forEach(System.out::println);
    }
    
    @Test
    public void testPassword() throws Exception {
        String special = "@#$%^&*-_!+=[]{}|\\:’,.?/`~\"();";
        var passwordValidation = Validation.<String, String>builder()
        .addValidation(p -> p.length() <8, "Too short password")
        .addValidation(p -> p.length() > 16, "Too long password")
        .addValidation(p -> p.chars().anyMatch(Character::isWhitespace), "Spaces are not allowed in the password")
        .addValidation(p -> !p.chars().anyMatch(Character::isDigit), "Missing digits in the password")
        .addValidation(p -> !p.chars().anyMatch(Character::isUpperCase), "Missing upper case chars")
        .addValidation(p -> !p.chars().anyMatch(Character::isLowerCase), "Missing lower case chars")
        .build();
    }
    
    
}
