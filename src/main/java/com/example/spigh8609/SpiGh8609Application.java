package com.example.spigh8609;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorSupport;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SpringBootApplication
public class SpiGh8609Application {

  public static void main(String[] args) {
    SpringApplication.run(SpiGh8609Application.class, args);
  }

  @Bean
  public TcpConnectionInterceptorFactoryChain tcpConnectionInterceptorFactoryChain(
      List<TcpConnectionInterceptorFactory> tcpConnectionInterceptorFactories) {
    TcpConnectionInterceptorFactoryChain chain = new TcpConnectionInterceptorFactoryChain();
    chain.setInterceptor(tcpConnectionInterceptorFactories.toArray(new TcpConnectionInterceptorFactory[0]));
    return chain;
  }

  @Bean
  public AbstractServerConnectionFactory serverConnectionFactory(
      TcpConnectionInterceptorFactoryChain chain) {
    return Tcp.netServer(1234)
        .deserializer(TcpCodecs.crlf())
        .interceptorFactoryChain(chain)
        .get();
  }

  @Bean
  public IntegrationFlow tcpServerInbound(AbstractServerConnectionFactory serverConnectionFactory,
      BlockingQueue<String> tcpServerReceivedMessageQueue) {
    return IntegrationFlows.from(Tcp.inboundAdapter(serverConnectionFactory))
        .transform(Transformers.objectToString())
        .log()
        .handle(m -> tcpServerReceivedMessageQueue.offer((String) m.getPayload()))
        .get();
  }

  @Bean
  public IntegrationFlow tcpServerOutbound(AbstractServerConnectionFactory serverConnectionFactory) {
    return IntegrationFlows.from("tcpServerOutboundChannel")
        .log()
        .handle(Tcp.outboundAdapter(serverConnectionFactory))
        .get();
  }

  @Bean
  public BlockingQueue<String> tcpServerReceivedMessageQueue() {
    return new LinkedBlockingQueue<>();
  }

  public static class CharacterAppendingInterceptor extends TcpConnectionInterceptorSupport {
    private final String value;

    private CharacterAppendingInterceptor(String value, ApplicationEventPublisher publisher) {
      super(publisher);
      this.value = value;
    }

    @Override public void send(Message<?> message) {
      // append specified character and delegate sending message using super class method
      byte[] payload = (byte[]) message.getPayload();
      super.send(MessageBuilder.withPayload((new String(payload) + value).getBytes()).copyHeaders(message.getHeaders())
          .build());
    }

    @Override public boolean onMessage(Message<?> message) {
      // append specified character and delegate handling message using super class method
      byte[] payload = (byte[]) message.getPayload();
      return super.onMessage(
          MessageBuilder.withPayload((new String(payload) + value).getBytes()).copyHeaders(message.getHeaders())
              .build());
    }

    static class Factory extends ApplicationObjectSupport implements TcpConnectionInterceptorFactory {
      private final String value;

      public Factory(String value) {
        this.value = value;
      }

      @Override public TcpConnectionInterceptorSupport getInterceptor() {
        return new CharacterAppendingInterceptor(value, obtainApplicationContext());
      }
    }
  }

  // The patch interceptor for binding a connection that wrapped all interceptor to TcpSender(in this case TcpSendingMessageHandler)
  public static class PatchInterceptor extends TcpConnectionInterceptorSupport {

    private PatchInterceptor(ApplicationEventPublisher publisher) {
      super(publisher);
    }

    @Override public void addNewConnection(TcpConnection connection) {
      //      Old patch code
      //      super.addNewConnection(connection);
      //      Optional.ofNullable(getSender()).ifPresent(x -> x.addNewConnection(getTheConnection()));

      // New patch code
      // Call addNewConnection of TcpSender with wrapped connection(interceptor) instead of passed connection using method argument
      getSenders().forEach(x -> x.addNewConnection(getTheConnection()));
    }

    static class Factory extends ApplicationObjectSupport implements TcpConnectionInterceptorFactory {

      @Override public TcpConnectionInterceptorSupport getInterceptor() {
        return new PatchInterceptor(obtainApplicationContext());
      }

    }

  }

}
