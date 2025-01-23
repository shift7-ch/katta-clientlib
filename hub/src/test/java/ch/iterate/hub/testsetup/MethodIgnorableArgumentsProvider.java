/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import java.util.Arrays;
import java.util.stream.Stream;

public class MethodIgnorableArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<MethodIgnorableSource> {

    private String[] methodNames;

    @Override
    public void accept(MethodIgnorableSource annotation) {
        methodNames = annotation.value();
    }

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Object testInstance = context.getTestInstance().orElse(null);
        return Arrays.stream(methodNames)
                .map(methodName -> ReflectionUtils.findMethod(testClass, methodName)
                        .orElseThrow(() -> new JUnitException("Could not find method: " + methodName)))
                .map(method -> ReflectionUtils.invokeMethod(method, testInstance))
                .flatMap(CollectionUtils::toStream)
                .map(MethodIgnorableArgumentsProvider::toArguments);
    }

    private static Arguments toArguments(Object item) {
        if(item instanceof Arguments) {
            return (Arguments) item;
        }
        if(item instanceof Object[]) {
            return Arguments.of((Object[]) item);
        }
        return Arguments.of(item);
    }
}
