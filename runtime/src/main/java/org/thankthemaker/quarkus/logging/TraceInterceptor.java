package org.thankthemaker.quarkus.logging;

import com.google.cloud.ServiceOptions;
import com.google.gson.JsonObject;
import io.opencensus.common.Scope;
import io.opencensus.contrib.http.HttpRequestContext;
import io.opencensus.contrib.http.HttpServerHandler;
import io.opencensus.contrib.http.util.HttpPropagationUtil;
import io.opencensus.trace.*;
import io.opencensus.trace.propagation.TextFormat;
import io.opencensus.trace.samplers.Samplers;
import org.jboss.logging.Logger;
import org.slf4j.MDC;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Traced
@Interceptor
public class TraceInterceptor {
  private static final Logger logger = Logger.getLogger(TraceInterceptor.class);
  private static final String PROJECT_ID = ServiceOptions.getDefaultProjectId();
  private static final Tracer tracer = Tracing.getTracer();

  @AroundInvoke
  public Object traceMethodInvocation(InvocationContext invocationContext) throws Exception {
    // update MDC resource context
    Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();

    MDC.put("resource", invocationContext.getMethod().getDeclaringClass().getSimpleName() + "." + invocationContext.getMethod().getName());

    Object methodInvocationResult = null;
    Span activeSpan = tracer.getCurrentSpan();
    if (!activeSpan.getContext().isValid()) {
      logger.debug("create span from requestContext");
      ContainerRequestContext requestContext = RestLoggingContextHolder.get().getRequestContext();
      if (requestContext.getHeaderString("X-Cloud-Trace-Context") == null) {
        logger.debug("no remote parent available");
        methodInvocationResult = createSpanForRequestAndContinue(invocationContext);
      } else {
        logger.debug("link span to remote parent");
        methodInvocationResult = createSpanForRemoteParentAndContinue(invocationContext);
      }
    } else {
      logger.debug("create span with explicit parent");
      try (Scope scope = tracer.spanBuilderWithExplicitParent(invocationContext.getMethod().getDeclaringClass().getSimpleName() + "." + invocationContext.getMethod().getName(), activeSpan).startScopedSpan()) {
        methodInvocationResult = logTraceAndContinue(invocationContext);
      }
    }
    printAuditLog(methodInvocationResult);
    //reset context map to not conflict with values set by a nested span
    MDC.setContextMap(copyOfContextMap);
    return methodInvocationResult;
  }

  private Object createSpanForRequestAndContinue(InvocationContext invocationContext) throws Exception {
    String httpResourceName = RestLoggingContextHolder.get().getRequestContext().getMethod() + getRequestUrl(RestLoggingContextHolder.get().getRequestContext().getUriInfo().getPath());
    try (Scope scope = tracer.spanBuilder(httpResourceName)
        .setRecordEvents(true)
        .setSampler(Samplers.alwaysSample()).startScopedSpan()) {
      return logTraceAndContinue(invocationContext);
    }
  }

  private Object createSpanForRemoteParentAndContinue(InvocationContext invocationContext) throws Exception {
    Response response = null;
    Throwable error = null;
    HttpServerHandler handler =
        new HttpServerHandler(
            Tracing.getTracer(), new XCloudTraceContextHeaderExtractor(), HttpPropagationUtil.getCloudTraceFormat(), new TextFormat.Getter<ContainerRequestContext>() {
          @Override
          public String get(ContainerRequestContext carrier, String key) {
            return carrier.getHeaderString(key);
          }
        },
            false /* true if it is public endpoint */);
    ContainerRequestContext requestContext = RestLoggingContextHolder.get().getRequestContext();
    HttpRequestContext context = handler.handleStart(requestContext, requestContext);
    try (Scope scope = tracer.withSpan(handler.getSpanFromContext(context))) {
      Object methodInvocationResult = logTraceAndContinue(invocationContext);
      if (methodInvocationResult instanceof Response){
        response = (Response) methodInvocationResult;
      }else if (methodInvocationResult instanceof Throwable){
        error = (Throwable) methodInvocationResult;
      }
      return methodInvocationResult;
    } finally {
      //TODO
      handler.handleEnd(context, requestContext, response, error);
    }
  }

  private Object logTraceAndContinue(InvocationContext invocationContext) throws Exception {
    Span currentSpan = tracer.getCurrentSpan();
    currentSpan.addAnnotation(invocationContext.getMethod().getName() + ":START");
    //addParameterAnnotation(invocationContext, currentSpan);
    TraceId traceId = currentSpan.getContext().getTraceId();
    SpanId spanId = currentSpan.getContext().getSpanId();
    org.jboss.logging.MDC.put("traceId", "projects/" + PROJECT_ID + "/traces/" + traceId.toLowerBase16());
    org.jboss.logging.MDC.put("spanId", spanId.toLowerBase16());

    Object object = invocationContext.proceed();

    currentSpan.addAnnotation(invocationContext.getMethod().getName() + ":END");
    currentSpan.end();
    return object;
  }

  private void addParameterAnnotation(InvocationContext invocationContext, Span currentSpan) {
    Parameter[] parameters = invocationContext.getMethod().getParameters();
    String params = Arrays.stream(parameters).map(parameter -> parameter.toString()).collect(Collectors.joining(","));
    if (!params.isEmpty()) {
      currentSpan.addAnnotation("Parameter(s):" + params);
    }
  }

  private String getRequestUrl(String path) {
    if (path.startsWith("//")) {
      return path.substring(1);
    } else {
      return path;
    }
  }

  private void printAuditLog(Object returnedObject) {
    long startTime = RestLoggingContextHolder.get().getStartTime();
    long timeTaken = System.currentTimeMillis() - startTime;
    MDC.put("duration", Long.toString(timeTaken));

    if (returnedObject instanceof Response) {
      JsonObject loggedRequest = new JsonObject();
      loggedRequest.addProperty("requestMethod", RestLoggingContextHolder.get().getRequestContext().getMethod());
      loggedRequest.addProperty("requestUrl", getRequestUrl(RestLoggingContextHolder.get().getRequestContext().getUriInfo().getPath()));
      loggedRequest.addProperty("requestSize", RestLoggingContextHolder.get().getRequestContext().getLength());
      loggedRequest.addProperty("userAgent", RestLoggingContextHolder.get().getRequestContext().getHeaderString("User-Agent"));
      loggedRequest.addProperty("status", (((Response) returnedObject)).getStatus());
      loggedRequest.addProperty("latency", String.valueOf(timeTaken));
      logger.info("\"httpRequest\":" + loggedRequest.getAsJsonObject());
    } else {
      logger.info("\"result\":\"" + returnedObject + "\"");
    }
  }
}
