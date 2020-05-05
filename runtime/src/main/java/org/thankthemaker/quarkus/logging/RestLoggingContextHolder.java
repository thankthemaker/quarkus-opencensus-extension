package org.thankthemaker.quarkus.logging;

public class RestLoggingContextHolder {
  private static final ThreadLocal<RestLoggingContext> REST_LOGGING_CONTEXT = new ThreadLocal<>();

  public static void set(RestLoggingContext restLoggingContext){
    REST_LOGGING_CONTEXT.set(restLoggingContext);
  }

  public static void unset(){
    REST_LOGGING_CONTEXT.remove();
  }

  public static RestLoggingContext get(){
    return REST_LOGGING_CONTEXT.get();
  }
}
