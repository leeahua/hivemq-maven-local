package co;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.services.AsyncClientService;
import com.hivemq.spi.services.BlockingClientService;
import com.hivemq.spi.services.ClientService;
import com.hivemq.spi.services.PluginExecutorService;
import d.CacheScoped;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

@CacheScoped
public class ClientServiceImpl implements ClientService {
    private final BlockingClientService blockingClientService;
    private final AsyncClientService asyncClientService;
    private final PluginExecutorService pluginExecutorService;

    @Inject
    public ClientServiceImpl(BlockingClientService blockingClientService,
                             AsyncClientService asyncClientService,
                             PluginExecutorService pluginExecutorService) {
        this.blockingClientService = blockingClientService;
        this.asyncClientService = asyncClientService;
        this.pluginExecutorService = pluginExecutorService;
    }

    public Set<String> getLocalConnectedClients() {
        return this.blockingClientService.getLocalConnectedClients();
    }

    public Set<String> getLocalDisconnectedClients() {
        return this.blockingClientService.getLocalDisconnectedClients();
    }

    public boolean isClientConnectedLocal(String clientId) {
        return this.blockingClientService.isClientConnectedLocal(clientId);
    }

    public Optional<ClientData> getLocalClientDataForClientId(String clientId) {
        return Optional.ofNullable(this.blockingClientService.getLocalClientData(clientId));
    }

    public ListenableFuture<Set<String>> getConnectedClients() {
        return this.asyncClientService.getConnectedClients();
    }

    public ListenableFuture<Set<String>> getDisconnectedClients() {
        return this.asyncClientService.getDisconnectedClients();
    }

    public ListenableFuture<Boolean> isClientConnected(String clientId) {
        return this.asyncClientService.isClientConnected(clientId);
    }

    public ListenableFuture<Optional<ClientData>> getClientDataForClientId(String clientId) {
        ListenableFuture future = this.asyncClientService.getClientData(clientId);
        SettableFuture settableFuture = SettableFuture.create();
        Futures.addCallback(future, new FutureCallback<ClientData>() {
            @Override
            public void onSuccess(@Nullable ClientData result) {
                settableFuture.set(Optional.ofNullable(result));
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }
}
