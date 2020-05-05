package org.thankthemaker.quarkus.logging;

import io.opencensus.contrib.http.HttpExtractor;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

public class XCloudTraceContextHeaderExtractor extends HttpExtractor {
  @Nullable
  @Override
  public String getRoute(Object o) {
    return null;
  }

  @Nullable
  @Override
  public String getUrl(Object o) {
    return ((ContainerRequestContext)o).getUriInfo().getPath();
  }

  @Nullable
  @Override
  public String getHost(Object o) {
    return ((ContainerRequestContext)o).getUriInfo().getRequestUri().getHost();
  }

  @Nullable
  @Override
  public String getMethod(Object o) {
    return ((ContainerRequestContext)o).getMethod();
  }

  @Nullable
  @Override
  public String getPath(Object o) {
    return ((ContainerRequestContext)o).getUriInfo().getPath();
  }

  @Nullable
  @Override
  public String getUserAgent(Object o) {
    return ((ContainerRequestContext)o).getHeaderString("User-Agent");
  }

  @Override
  public int getStatusCode(@Nullable Object o) {
    if (o == null)
      return 0;
    return ((Response)o).getStatus();
  }
}
