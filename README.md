# spi-gh-8609
Repro project for spring-integration issue https://github.com/spring-projects/spring-integration/issues/8609.

> **NOTE:**
>
> This issue will be fixed via 6.1.0, 6.0.6 and 5.5.18.

## How to use the patch interceptor

This project provide the workaround sample interceptor([`SpringIntegrationIssue8609PatchInterceptor`](https://github.com/kazuki43zoo/spi-gh-8609/blob/main/src/main/java/com/example/spigh8609/SpringIntegrationIssue8609PatchInterceptor.java)) for fix this issue.
The [`SpringIntegrationIssue8609PatchInterceptor`](https://github.com/kazuki43zoo/spi-gh-8609/blob/main/src/main/java/com/example/spigh8609/SpringIntegrationIssue8609PatchInterceptor.java)'s factory should be applied to the `TcpConnectionInterceptorFactoryChain` at last.

Introduce sample bean definitions that applied patch interceptor to your application.

### Define beans with Java Configuration

```java
@Bean
public AbstractServerConnectionFactory serverConnectionFactory(
    List<TcpConnectionInterceptorFactory> tcpConnectionInterceptorFactories) {
  return Tcp.netServer(1234)
      .deserializer(TcpCodecs.crlf())
      .interceptorFactoryChain(tcpConnectionInterceptorFactoryChain(tcpConnectionInterceptorFactories))
      .get();
}

private TcpConnectionInterceptorFactoryChain tcpConnectionInterceptorFactoryChain(
    List<TcpConnectionInterceptorFactory> tcpConnectionInterceptorFactories) {
  TcpConnectionInterceptorFactoryChain chain = new TcpConnectionInterceptorFactoryChain();
  chain.setInterceptors(tcpConnectionInterceptorFactories.toArray(new TcpConnectionInterceptorFactory[0]));
  return chain;
}

@Bean
@Order(1)
public TcpConnectionInterceptorFactory myInterceptor1() {
  return new MyInterceptor1.Factory();
}

@Bean
@Order(2)
public TcpConnectionInterceptorFactory myInterceptor2() {
  return new MyInterceptor2.Factory();
}

@Bean
@Order(3) // ★★★Should set order at last★★★
public TcpConnectionInterceptorFactory patchInterceptor() {
  return new SpringIntegrationIssue8609PatchInterceptor.Factory();
}

```

### Define beans with XML Configuration

```xml
<ip:tcp-connection-factory id="serverConnectionFactory"
  type="server"
  port="1234"
  serializer="crLfSerializer"
  deserializer="crLfSerializer"
  interceptor-factory-chain="tcpConnectionInterceptorFactoryChain" />

<bean id="tcpConnectionInterceptorFactoryChain" class="org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain">
  <property name="interceptors">
    <array>
      <bean class="com.example.MyInterceptor1.Factory"/>
      <bean class="com.example.MyInterceptor2.Factory"/>
      <bean class="com.example.spigh8609.SpringIntegrationIssue8609PatchInterceptor.Factory"/> <!-- ★★★Should define the bean at last★★★ -->
    </array>
  </property>
</bean>
```

## Articles

* [Qiita(Japanese only)](https://qiita.com/kazuki43zoo/items/870486eff86aac3b1610)
