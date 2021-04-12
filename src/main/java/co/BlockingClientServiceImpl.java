package co;

import com.google.inject.Inject;
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.services.AsyncClientService;
import com.hivemq.spi.services.BlockingClientService;
import d.CacheScoped;

import java.util.Set;

@CacheScoped
public class BlockingClientServiceImpl implements BlockingClientService {
    private final AsyncClientService asyncClientService;

    @Inject
    public BlockingClientServiceImpl(AsyncClientService asyncClientService) {
        this.asyncClientService = asyncClientService;
    }

    public Set<String> getLocalConnectedClients() {
        try {
            return this.asyncClientService.getLocalConnectedClients().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getLocalDisconnectedClients() {
        try {
            return this.asyncClientService.getLocalDisconnectedClients().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClientConnectedLocal(String clientId) {
        try {
            return this.asyncClientService.isClientConnectedLocal(clientId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClientData getLocalClientData(String clientId) {
        try {
            return this.asyncClientService.getLocalClientData(clientId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getConnectedClients() {
        try {
            return this.asyncClientService.getConnectedClients().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getDisconnectedClients() {
        try {
            return this.asyncClientService.getDisconnectedClients().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClientConnected(String clientId) {
        try {
            return this.asyncClientService.isClientConnected(clientId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClientData getClientData(String clientId) {
        try {
            return this.asyncClientService.getClientData(clientId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
