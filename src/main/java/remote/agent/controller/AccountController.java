package remote.agent.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
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
    private WebSocketAgentClient webSocketAgentClient;

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

    @POST
    @Path("/agent/wsconnect")
    @Consumes(MediaType.TEXT_PLAIN)
    public void connectToServer(@QueryParam("connectionId") String connectionId) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = URI.create(String.format("ws://192.168.88.2:5679/wsremote/%s",connectionId));
            container.connectToServer(webSocketAgentClient,uri);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


}
