/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// https://javax0.wordpress.com/2021/05/04/creating-a-junit-5-executioncondition/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledInJenkinsIfNotMacOS.DisabledInJenkinsIfNotMacOSCondition.class)
public @interface DisabledInJenkinsIfNotMacOS {
    class DisabledInJenkinsIfNotMacOSCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext extensionContext) {
            if(java.lang.System.getenv("JENKINS_HOME") != null && !java.lang.System.getProperty("os.name").toLowerCase().contains("mac")) {
                return ConditionEvaluationResult.disabled("Docker runners only for linux nodes in Jenkins.");
            }
            return ConditionEvaluationResult.enabled("Docker runners should be present");
        }
    }
}
