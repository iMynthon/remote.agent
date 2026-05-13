package remote.agent.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import remote.agent.dto.LoginResponse;
import remote.agent.dto.RegistrationRequest;
import remote.agent.restclient.AccountRestClient;

@Slf4j
@Path("api/v1/agent/account")
@ApplicationScoped
public class AccountController {

    @RestClient
    @Inject
    public AccountRestClient accountRestClient;

    @POST
    @Path("/reg")
    @Produces(MediaType.APPLICATION_JSON)
    public String requesterToReg(RegistrationRequest registrationRequest) {
        String value = accountRestClient.reg(registrationRequest);
        log.info(value);
        return value;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LoginResponse requesterToLogin(RegistrationRequest registrationRequest) {
        log.info("Calling rest method requesterToLogin param: {}", registrationRequest);
        return accountRestClient.login(registrationRequest);
    }
}
