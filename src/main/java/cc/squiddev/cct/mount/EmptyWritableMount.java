package cc.squiddev.cct.mount;

import cc.squiddev.cct.stub.WritableByteChannel;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.filesystem.EmptyMount;

import javax.annotation.Nonnull;
import java.io.IOException;

public class EmptyWritableMount extends EmptyMount implements IWritableMount {
    @Override
    public void makeDirectory(@Nonnull String path) throws IOException {
        throw new IOException("Access denied");
    }

    @Override
    public void delete(@Nonnull String path) throws IOException {
        throw new IOException("Access denied");
    }

    @Nonnull
    @Override
    public WritableByteChannel openChannelForWrite(@Nonnull String path) throws IOException {
        throw new IOException("Access denied");
    }

    @Nonnull
    @Override
    public WritableByteChannel openChannelForAppend(@Nonnull String path) throws IOException {
        throw new IOException("Access denied");
    }

    @Override
    public long getRemainingSpace() {
        return 0;
    }
}
