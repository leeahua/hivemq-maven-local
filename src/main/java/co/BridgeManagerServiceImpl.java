package co;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Singleton;
import com.hivemq.spi.bridge.Bridge;
import com.hivemq.spi.bridge.State;
import com.hivemq.spi.services.BridgeManagerService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class BridgeManagerServiceImpl implements BridgeManagerService {
    public void addBridge(Bridge paramBridge) {
        throw new UnsupportedOperationException();
    }

    public void removeBridge(Bridge paramBridge) {
        throw new UnsupportedOperationException();
    }

    public ListenableFuture<Void> startBridge(Bridge paramBridge) {
        throw new UnsupportedOperationException();
    }

    public ListenableFuture<Void> stopBridge(Bridge paramBridge) {
        throw new UnsupportedOperationException();
    }

    public Collection<Bridge> listBridges() {
        throw new UnsupportedOperationException();
    }

    public ListenableFuture<List<Void>> stopAllBridges() {
        throw new UnsupportedOperationException();
    }

    public Optional<State> getBridgeState(Bridge paramBridge) {
        throw new UnsupportedOperationException();
    }
}
