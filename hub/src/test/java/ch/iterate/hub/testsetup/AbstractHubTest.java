/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import ch.cyberduck.test.IntegrationTest;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("hub")
@IntegrationTest
@interface HubIntegrationTest {
}

@HubIntegrationTest
@DisabledInJenkinsIfNotMacOS
public abstract class AbstractHubTest {

}

