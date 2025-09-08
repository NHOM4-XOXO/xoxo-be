package com.nhom4.xoxo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    private Class<? extends Enum<?>> enumClass;
    private boolean ignoreCase;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.ignoreCase = constraintAnnotation.ignoreCase();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null values
        }

        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            String enumValue = enumConstant.name();
            if (ignoreCase ? enumValue.equalsIgnoreCase(value) : enumValue.equals(value)) {
                return true;
            }
        }
        
        // Customize error message with valid values
        StringBuilder validValues = new StringBuilder();
        for (Enum<?> enumConstant : enumConstants) {
            if (validValues.length() > 0) {
                validValues.append(", ");
            }
            validValues.append(enumConstant.name());
        }
        
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
            "Giá trị phải là một trong: " + validValues.toString()
        ).addConstraintViolation();
        
        return false;
    }
}


