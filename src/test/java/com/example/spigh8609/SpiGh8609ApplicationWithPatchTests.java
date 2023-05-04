package com.example.spigh8609;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = { SpiGh8609Application.class, SpiGh8609ApplicationWithPatchTests.LocalContext.class })
@DirtiesContext
class SpiGh8609ApplicationWithPatchTests {

  @Autowired
  MessageChannel tcpServerOutboundChannel;

  @Autowired
  AbstractServerConnectionFactory connectionFactory;

  @Autowired
  BlockingQueue<String> tcpServerReceivedMessageQueue;

  @Test
  void connectAndSend() throws IOException, InterruptedException {
    System.out.println("------------connectAndSend-------------");
    // connect
    try (Socket s = new Socket("localhost", 1234)) {
      // send message to tcp server
      s.getOutputStream().write("abc".getBytes());
      s.getOutputStream().write("\r\n".getBytes());
      s.getOutputStream().flush();

      // get received message from queue
      String message = tcpServerReceivedMessageQueue.poll(1, TimeUnit.SECONDS);
      System.out.println(message);

      // Work fine!!
      Assertions.assertThat(message).isEqualTo("abc12");
    }
  }

  @Test
  void connectAndReceive() throws IOException {
    System.out.println("------------connectAndReceive-------------");
    // connect
    try (Socket s = new Socket("localhost", 1234);
        BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

      // send message to remote client using server connection
      connectionFactory.getOpenConnectionIds().stream().findAny().ifPresent(
          connectionId -> tcpServerOutboundChannel.send(
              MessageBuilder.withPayload("abc".getBytes()).setHeader(IpHeaders.CONNECTION_ID, connectionId).build()));

      // receive message
      String message = r.readLine();
      System.out.println(message);

      // Work fine!!
      Assertions.assertThat(message).isEqualTo("abc21");
    }
  }

  static class LocalContext {
    @Bean
    @Order(1)
    public TcpConnectionInterceptorFactory characterAppendingInterceptor1() {
      return new SpiGh8609Application.CharacterAppendingInterceptor.Factory("1");
    }

    @Bean
    @Order(2)
    public TcpConnectionInterceptorFactory characterAppendingInterceptor2() {
      return new SpiGh8609Application.CharacterAppendingInterceptor.Factory("2");
    }

    @Bean
    @Order(3)
    // Apply patch interceptor for binding a connection that wrapped all interceptor to TcpSender(in this case TcpSendingMessageHandler)
    public TcpConnectionInterceptorFactory patchInterceptor() {
      return new SpringIntegrationIssue8609PatchInterceptor.Factory();
    }
  }
}
