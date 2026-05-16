package remote.agent.controller;

import io.quarkus.websockets.next.*;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import remote.agent.service.StreamingDesktopService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@WebSocketClient(path = "/wsremote/{connectionId}")
public class WebSocketAgentClient {

    @Inject
    private StreamingDesktopService streamingDesktopService;

    private Map<String,WebSocketClientConnection> initiatedConnections = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(WebSocketClientConnection webSocketClientConnection, @PathParam("connectionId") String connectionId) {
        log.info("Calling method ws OnOpen - {} -{}", webSocketClientConnection.id(), connectionId);
        if(webSocketClientConnection.isOpen()) {
            initiatedConnections.put(connectionId, webSocketClientConnection);
        }
    }

    @OnBinaryMessage
    public void onBinaryMessage(WebSocketClientConnection webSocketClientConnection,@PathParam("connectionId") String connectionId) {
        log.info("Calling method ws onBinaryMessage - {} -{}",webSocketClientConnection.id(), connectionId);
        streamingDesktopService.startStreamingRemoteScreen(webSocketClientConnection, connectionId)
                .subscribe().with(
                        success -> log.info("Streaming Remote Screen has been started - {}", connectionId),
                        failure -> log.error("Failed to start streaming - {}", failure.getMessage())
                );
    }

    @OnClose
    public void onClose(WebSocketClientConnection webSocketClientConnection,@PathParam("connectionId") String connectionId) {
        log.info("Calling method ws OnClose - {} - {}",webSocketClientConnection.id(),connectionId);
        streamingDesktopService.stopStreamingRemoteScreen(connectionId)
                .subscribe().with(
                        v -> log.info("Streaming stopped for {}", connectionId),
                        err -> log.error("Failed to stop streaming", err)
                );
    }

    @OnError
    public void onError(Throwable throwable) {
        log.error("WebSocket connection error: {}",throwable.getMessage());
    }
}
