package org.example.chessmystic.Config.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class ChessWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(ChessWebSocketConfig.class);

    @Value("${websocket.stomp.relay.enabled:false}")
    private boolean relayEnabled;

    @Value("${spring.websocket.stomp.relay.host:localhost}")
    private String relayHost;

    @Value("${spring.websocket.stomp.relay.port:61613}")
    private int relayPort;

    @Value("${spring.websocket.stomp.relay.client-login:guest}")
    private String relayLogin;

    @Value("${spring.websocket.stomp.relay.client-passcode:guest}")
    private String relayPasscode;

    @Bean
    public TaskScheduler chessWebSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("chess-websocket-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chess-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(20000)
                .setDisconnectDelay(3000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    String sessionId = accessor.getSessionId();

                    if (accessor.getDestination() != null) {
                        String original = accessor.getDestination();
                        if (relayEnabled) {
                            String updated = original;

                            // Do not touch user-destinations handled by Spring itself
                            boolean isUserDest = original.startsWith("/user/");

                            if (!isUserDest) {
                                if (original.startsWith("/queue/")) {
                                    String tail = original.substring("/queue/".length());
                                    if (tail.contains("/")) {
                                        updated = "/queue/" + tail.replace('/', '.');
                                    }
                                } else if (original.startsWith("/topic/")) {
                                    // Map to explicit amq.topic exchange and dot-separated routing key
                                    String tail = original.substring("/topic/".length());
                                    updated = "/exchange/amq.topic/" + tail.replace('/', '.');
                                }
                            }

                            if (!updated.equals(original)) {
                                log.debug("Rewriting STOMP destination for session {}: {} -> {}", sessionId, original, updated);
                                accessor.setDestination(updated);
                            }
                        }
                    }

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        log.info("STOMP session connecting: {}", sessionId);
                        accessor.getSessionAttributes().put("CONNECTED_TIME", System.currentTimeMillis());
                        accessor.getSessionAttributes().put("ACTIVE", true);

                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        log.info("STOMP session disconnecting: {}", sessionId);
                        accessor.getSessionAttributes().put("DISCONNECTING", true);
                        accessor.getSessionAttributes().put("ACTIVE", false);

                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        log.debug("STOMP subscription for session {}: {}", sessionId, accessor.getDestination());

                    } else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                        log.debug("STOMP unsubscription for session {}: {}", sessionId, accessor.getSubscriptionId());
                    }
                }
                return message;
            }

            @Override
            public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    String sessionId = accessor.getSessionId();

                    if (StompCommand.CONNECT.equals(accessor.getCommand()) && sent) {
                        log.info("STOMP session connected successfully: {}", sessionId);

                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        log.info("STOMP session disconnect completed: {}", sessionId);
                        accessor.getSessionAttributes().put("COMPLETED", true);
                    }
                }
            }

            @Override
            public void afterSendCompletion(Message<?> message, MessageChannel channel,
                                            boolean sent, Exception ex) {
                if (ex != null) {
                    StompHeaderAccessor accessor =
                            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor != null) {
                        log.warn("Error sending message for session {}: {}",
                                accessor.getSessionId(), ex.getMessage());
                    }
                }
            }
        });

        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(20)
                .queueCapacity(200)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && accessor.getSessionAttributes() != null) {
                    Boolean disconnecting = (Boolean) accessor.getSessionAttributes().get("DISCONNECTING");
                    Boolean completed = (Boolean) accessor.getSessionAttributes().get("COMPLETED");

                    if (Boolean.TRUE.equals(disconnecting) || Boolean.TRUE.equals(completed)) {
                        log.debug("Blocking message to disconnecting/completed session: {}",
                                accessor.getSessionId());
                        return null;
                    }
                }

                // Normalize outbound destinations too for consistency
                if (accessor != null && accessor.getDestination() != null && relayEnabled) {
                    String original = accessor.getDestination();
                    String updated = original;

                    boolean isUserDest = original.startsWith("/user/");

                    if (!isUserDest) {
                        if (original.startsWith("/queue/")) {
                            String tail = original.substring("/queue/".length());
                            if (tail.contains("/")) {
                                updated = "/queue/" + tail.replace('/', '.');
                            }
                        } else if (original.startsWith("/topic/")) {
                            String tail = original.substring("/topic/".length());
                            updated = "/exchange/amq.topic/" + tail.replace('/', '.');
                        }
                    }

                    if (!updated.equals(original)) {
                        log.debug("Rewriting outbound STOMP destination: {} -> {}", original, updated);
                        accessor.setDestination(updated);
                    }
                }
                return message;
            }
        });

        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(20)
                .queueCapacity(200)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(256 * 1024)
                .setSendTimeLimit(20 * 1000)
                .setSendBufferSizeLimit(2 * 512 * 1024)
                .setTimeToFirstMessage(30 * 1000);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (relayEnabled) {
            try {
                log.info("Configuring STOMP broker relay at {}:{} (login={})", relayHost, relayPort, relayLogin);
                config.enableStompBrokerRelay("/topic", "/queue", "/exchange")
                        .setRelayHost(relayHost)
                        .setRelayPort(relayPort)
                        .setClientLogin(relayLogin)
                        .setClientPasscode(relayPasscode)
                        .setSystemLogin(relayLogin)
                        .setSystemPasscode(relayPasscode)
                        .setVirtualHost("/")
                        .setSystemHeartbeatSendInterval(10000)
                        .setSystemHeartbeatReceiveInterval(10000)
                        .setTaskScheduler(chessWebSocketTaskScheduler())
                        .setAutoStartup(true);

                config.setApplicationDestinationPrefixes("/app");
                config.setUserDestinationPrefix("/user");

            } catch (Exception e) {
                log.error("Failed to configure STOMP broker relay, falling back to simple broker: {}", e.getMessage());
                config.enableSimpleBroker("/topic", "/queue");
                config.setApplicationDestinationPrefixes("/app");
                config.setUserDestinationPrefix("/user");
            }
        } else {
            log.info("Using simple in-memory message broker (relay disabled)");
            config.enableSimpleBroker("/topic", "/queue");
            config.setApplicationDestinationPrefixes("/app");
            config.setUserDestinationPrefix("/user");
        }
    }
}