package com.example.spigh8609;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptor;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorSupport;

/**
 * The patch interceptor for fixing the <a href="https://github.com/spring-projects/spring-integration/issues/8609">GH-8609</a>.
 * <p>
 * This interceptor's factory should be applied to the {@link TcpConnectionInterceptorFactoryChain} at last.
 * </p>
 */
public class SpringIntegrationIssue8609PatchInterceptor extends TcpConnectionInterceptorSupport {

  private SpringIntegrationIssue8609PatchInterceptor(ApplicationEventPublisher publisher) {
    super(publisher);
  }

  @Override
  public void addNewConnection(TcpConnection connection) {
    // Call addNewConnection of TcpSender with wrapped connection(interceptor) instead of passed connection using method argument
    getSenders().forEach(x -> x.addNewConnection(getTheConnection()));
  }

  public static class Factory extends ApplicationObjectSupport implements TcpConnectionInterceptorFactory {

    @Override
    public TcpConnectionInterceptorSupport getInterceptor() {
      return new SpringIntegrationIssue8609PatchInterceptor(obtainApplicationContext());
    }

  }

}