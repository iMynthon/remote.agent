package remote.agent.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import remote.agent.service.StreamingDesktopService;
import java.nio.ByteBuffer;

@Slf4j
@ClientEndpoint
public class WebSocketAgentClient {

    @Inject
    private StreamingDesktopService streamingDesktopService;

    private Session session;

    private String connectionId;

    @OnOpen
    public void onOpen(Session session, @PathParam("connectionId") String connectionId) {
        this.session = session;
        this.connectionId = connectionId;
        log.info("Calling method ws OnOpen - {} -{}", session.getId(), this.connectionId);
        streamingDesktopService.startStreamingRemoteScreen(this, connectionId)
                .subscribe().with(
                        success -> log.info("Streaming Remote Screen has been started - {}", connectionId),
                        failure -> log.error("Failed to start streaming - {}", failure.getMessage())
                );
    }

    @OnClose
    public void onClose(Session session, @PathParam("connectionId") String connectionId) {
        this.connectionId = connectionId;
        log.info("Calling method ws OnClose - {} - {}", session.getId(), this.connectionId);
        streamingDesktopService.stopStreamingRemoteScreen(this.connectionId)
                .subscribe().with(
                        v -> log.info("Streaming stopped for {}", this.connectionId),
                        err -> log.error("Failed to stop streaming", err)
                );
    }

    @OnError
    public void onError(Throwable throwable) {
        log.error("WebSocket connection error: {} - {}", connectionId, throwable.getMessage());
    }

    public Uni<Void> sendBinary(byte[] data) {
        if (session == null || !session.isOpen()) {
            return Uni.createFrom().voidItem();
        }
        return Uni.createFrom().emitter(emitter -> {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(data), result -> {
                if (result.isOK()) {
                    emitter.complete(null);
                } else {
                    emitter.fail(new RuntimeException("WebSocket send failed", result.getException()));
                }
            });
        });
    }
}
