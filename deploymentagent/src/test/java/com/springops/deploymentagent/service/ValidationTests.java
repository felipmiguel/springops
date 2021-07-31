package com.springops.deploymentagent.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@SpringBootTest
public class ValidationTests {
    @Test
    void instantiateValidator(){
        var factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        assertNotNull(validator);
    }
}
