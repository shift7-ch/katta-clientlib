/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DirectoryDelimiterPathContainerService;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.transfer.TransferStatus;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class HubVaultDeleteFeature implements Delete {

    private final Delete storage;

    public HubVaultDeleteFeature(final Delete storage) {
        this.storage = storage;
    }

    @Override
    public void delete(final List<Path> files, final PasswordCallback prompt, final Callback callback) throws BackgroundException {
        storage.delete(files, prompt, callback);
    }

    @Override
    public void delete(final Map<Path, TransferStatus> files, final PasswordCallback prompt, final Callback callback) throws BackgroundException {
        storage.delete(files, prompt, callback);
    }

    @Override
    public void preflight(final Path file) throws BackgroundException {
        if(new DirectoryDelimiterPathContainerService().isContainer(file)) {
            throw new UnsupportedException(MessageFormat.format(LocaleFactory.localizedString("Cannot delete {0}", "Error"), file.getName())).withFile(file);
        }
        storage.preflight(file);
    }

    @Override
    public EnumSet<Flags> features(final Path file) {
        return storage.features(file);
    }
}
