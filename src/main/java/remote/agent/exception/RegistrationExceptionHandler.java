package remote.agent.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RegistrationExceptionHandler implements ExceptionMapper<RegistrationException> {
    @Override
    public Response toResponse(RegistrationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),exception.getMessage()))
                .build();
    }
}
