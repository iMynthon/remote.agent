package remote.agent.restclient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import remote.agent.dto.LoginResponse;
import remote.agent.dto.RegistrationRequest;
import remote.agent.dto.RegistrationResponse;

import java.util.Optional;

@RegisterRestClient(baseUri = "http://192.168.88.2:5679/api/v1/auth", configKey = "account")
public interface AccountRestClient {

    @POST
    @Path("/reg")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RegistrationResponse reg(RegistrationRequest registrationRequest);

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    LoginResponse login(RegistrationRequest registrationRequest);

}
