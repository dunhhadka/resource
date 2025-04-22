package com.example.storesecurity.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ConfigurationOnAuthenticationCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var result = Binder.get(context.getEnvironment()).bind("security", SecurityProperties.class);
        var authMethods = result
                .orElse(new SecurityProperties())
                .getAuthMethods();
        var condition = metadata.getAnnotations().get(ConfigurationOnAuthenticationMethod.class).synthesize();
        if (condition.value() != null && authMethods.contains(condition.value())) {
            return ConditionOutcome.match();
        }
        return ConditionOutcome.noMatch("Not found authentication method");
    }
}
