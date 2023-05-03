package com.example.spigh8609;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@DirtiesContext
class SpiGh8609ApplicationWithoutInterceptorTests {

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
      Assertions.assertThat(message).isEqualTo("abc");
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
      Assertions.assertThat(message).isEqualTo("abc");
    }
  }

}
