package org.thankthemaker.quarkus.logging;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import javax.ws.rs.container.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Provider
public class RestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger logger = Logger.getLogger(RestLoggingFilter.class);
    private static final String X_REQUEST_ID = "x-request-id";
    private static final String X_CORRELATION_ID = "x-correlation-id";
    private static final String X_CLIENT_ID = "x-client-id";
    private static final String X_CLIENT_VERSION = "x-client-version";
    private static final String X_OS_VERSION = "x-client-os-version";

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RestLoggingContext restLoggingContext = new RestLoggingContext(System.currentTimeMillis(), requestContext);
        RestLoggingContextHolder.set(restLoggingContext);

        logger.debug("\"RequestHeaders\":\"" + requestContext.getHeaders().keySet().stream().collect(Collectors.joining(",")) + "\"");

        // fill MDC
        MDC.put("correlationId", getCorrelationId(requestContext));
        MDC.put("requestId", getRequestId(requestContext));
        MDC.put("clientId", getClientId(requestContext));
        MDC.put("clientVersion", getClientVersion(requestContext));
        MDC.put("clientOsVersion", getClientOsVersion(requestContext));
        MDC.put("resource", resourceInfo.getResourceClass().getSimpleName() + "." + resourceInfo.getResourceMethod().getName());
        MDC.put("duration", 0);

    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    }

    private String getCorrelationId(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(X_CORRELATION_ID);
        if (correlationId == null) {
            correlationId = "N/A";
        }
        return correlationId;
    }

    private String getRequestId(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(X_REQUEST_ID);
        if (requestId == null) {
            requestId = "GN-" + UUID.randomUUID().toString();
        }
        return requestId;
    }

    private String getClientId(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(X_CLIENT_ID);
        if (requestId == null) {
            requestId = "N/A";
        }
        return requestId;
    }

    private String getClientVersion(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(X_CLIENT_VERSION);
        if (requestId == null) {
            requestId = "N/A";
        }
        return requestId;
    }

    private String getClientOsVersion(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(X_OS_VERSION);
        if (requestId == null) {
            requestId = "N/A";
        }
        return requestId;
    }
}