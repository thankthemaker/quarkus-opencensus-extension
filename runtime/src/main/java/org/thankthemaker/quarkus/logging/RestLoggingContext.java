package org.thankthemaker.quarkus.logging;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.ws.rs.container.ContainerRequestContext;

@RegisterForReflection
public class RestLoggingContext {
  private long startTime;
  private ContainerRequestContext requestContext;

  public RestLoggingContext() {
  }

  public RestLoggingContext(long startTime, ContainerRequestContext requestContext) {
    this.startTime = startTime;
    this.requestContext = requestContext;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public ContainerRequestContext getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(ContainerRequestContext requestContext) {
    this.requestContext = requestContext;
  }
}
