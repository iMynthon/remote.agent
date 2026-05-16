package remote.agent.controller;

import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketConnector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import remote.agent.dto.LoginResponse;
import remote.agent.dto.RegistrationRequest;
import remote.agent.dto.RegistrationResponse;
import remote.agent.service.AccountService;

import java.net.URI;

@Slf4j
@Path("api/v1/agent/account")
@ApplicationScoped
public class AccountController {

    @Inject
    private AccountService accountService;

    @Inject
    private BasicWebSocketConnector connector;

    @POST
    @Path("/reg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RegistrationResponse requesterToReg(RegistrationRequest registrationRequest) {
       return accountService.getRegisteredAccount(registrationRequest);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LoginResponse requesterToLogin(RegistrationRequest registrationRequest) {
         return accountService.getLogin(registrationRequest);
    }

    @GET
    @Path("/wsconnect/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response connectToServer(@PathParam("connectionId") String connectionId) {
        log.info("Connecting to server with connectionId {}", connectionId);
        try {
            URI uri = URI.create(String.format("ws://192.168.88.2:5679/wsremote/%s",connectionId));
            log.info("Connected to server with uri {}", uri.getPath());
            connector.baseUri(uri)
                    .connectAndAwait();
            return Response.ok().entity("Подключение инициировано").build();
        } catch (Exception e) {
            log.error("Ошибка подключения агента для ID {}: {}", connectionId, e.getMessage());
            return Response.serverError().entity("Ошибка подключения: " + e.getMessage()).build();
        }
    }


}
