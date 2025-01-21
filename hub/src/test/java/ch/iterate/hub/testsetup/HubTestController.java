/*
 * Copyright (c) 2022 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import ch.cyberduck.core.AbstractController;
import ch.cyberduck.core.threading.MainAction;

public class HubTestController extends AbstractController {

    @Override
    public void invoke(final MainAction runnable, final boolean wait) {
        runnable.run();
    }
}
