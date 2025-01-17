/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core.callback;

import ch.cyberduck.core.threading.AbstractBackgroundAction;

import ch.iterate.hub.core.CreateVaultBookmarkAction;

public interface CreateVaultCallback {

    public AbstractBackgroundAction<Void> create(final CreateVaultBookmarkAction action);
}
