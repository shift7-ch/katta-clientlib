/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @MethodSource} is an {@link ArgumentsSource} which provides access
 * to values returned by {@linkplain #value() methods} of the class in
 * which this annotation is declared.
 * <p>
 * <p>By default such methods must be {@code static} unless the test class is
 * annotated with
 * {@link org.junit.jupiter.api.TestInstance @TestInstance(Lifecycle.PER_CLASS)}.
 * <p>
 * <p>The values returned by such methods will be provided as arguments to the
 * annotated {@code @ParameterizedTest} method.
 *
 * @see ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedTest
 * @since 5.0
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(MethodIgnorableArgumentsProvider.class)
public @interface MethodIgnorableSource {

    /**
     * The names of the test class methods to use as sources for arguments; must
     * not be empty.
     */
    String[] value();
}
