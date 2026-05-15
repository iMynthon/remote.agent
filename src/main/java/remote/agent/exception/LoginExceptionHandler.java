package remote.agent.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LoginExceptionHandler implements ExceptionMapper<LoginException> {
    @Override
    public Response toResponse(LoginException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorMessage(Response.Status.UNAUTHORIZED.getStatusCode(),exception.getMessage()))
                .build();
    }
}
