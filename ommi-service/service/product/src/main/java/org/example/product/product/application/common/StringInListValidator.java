package org.example.product.product.application.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class StringInListValidator implements
        ConstraintValidator<StringInList, String> {

    private StringInList annotation;

    @Override
    public void initialize(StringInList constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String input, ConstraintValidatorContext context) {
        if (annotation.allowBlank() && StringUtils.isEmpty(input)) {
            return true;
        } else if (ArrayUtils.contains(annotation.array(), input)) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        var message = "%s is not in [%s]".formatted(
                input,
                StringUtils.join(annotation.array(), ", "));
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();

        return false;
    }
}
