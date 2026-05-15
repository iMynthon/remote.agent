package remote.agent.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

@Provider
public class ClientWebApplicationExceptionHandler implements ExceptionMapper<ClientWebApplicationException> {
    @Override
    public Response toResponse(ClientWebApplicationException clientWebApplicationException) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),clientWebApplicationException.getMessage()))
                .build();
    }
}
