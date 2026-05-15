package remote.agent.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import remote.agent.dto.LoginResponse;
import remote.agent.dto.RegistrationRequest;
import remote.agent.dto.RegistrationResponse;
import remote.agent.exception.ErrorMessage;
import remote.agent.exception.LoginException;
import remote.agent.exception.RegistrationException;
import remote.agent.restclient.AccountRestClient;

@Slf4j
@ApplicationScoped
public class AccountService {

    private static final String errorMessage = "Ошибка запроса или сервера, повторите попытку позже";

    @Inject
    @RestClient
    public AccountRestClient accountRestClient;

    public RegistrationResponse getRegisteredAccount(RegistrationRequest registrationRequest) {
        log.info("Calling getRegisteredAccount param - {}", registrationRequest);
        try {
            return accountRestClient.reg(registrationRequest);
        }catch (WebApplicationException wex){
            ErrorMessage responseEntity = wex.getResponse().readEntity(ErrorMessage.class);
            log.info("getRegisteredAccount - {} - {}", wex.getResponse().getStatus(), responseEntity);
            throw new RegistrationException(responseEntity.message() != null ? responseEntity.message() : errorMessage);
        }
    }

    public LoginResponse getLogin(RegistrationRequest registrationRequest) {
        log.info("Calling getLogin param - {}", registrationRequest);
        try {
            return accountRestClient.login(registrationRequest);
        } catch (WebApplicationException wex){
            ErrorMessage responseEntity = wex.getResponse().readEntity(ErrorMessage.class);
            log.info("getLogin exception - {} - {}", wex.getResponse().getStatus(), responseEntity);
            throw new LoginException(responseEntity.message() != null ? responseEntity.message() : errorMessage);
        }
    }
}
