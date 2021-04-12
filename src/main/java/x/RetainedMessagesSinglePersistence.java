package x;

import bz.RetainedMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;

import java.util.Set;

public interface RetainedMessagesSinglePersistence {
    long size();

    ListenableFuture<Void> remove(@NotNull String topic);

    ListenableFuture<RetainedMessage> getWithoutWildcards(@NotNull String topic);

    ListenableFuture<Void> addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage);

    @NotNull
    ImmutableList<ListenableFuture<Set<String>>> getTopics(@NotNull String topic);
}
