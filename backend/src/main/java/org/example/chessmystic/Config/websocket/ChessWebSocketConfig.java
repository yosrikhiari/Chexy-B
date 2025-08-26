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
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
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

    @Value("${spring.websocket.stomp.relay.host:localhost}")
    private String relayHost;

    @Value("${spring.websocket.stomp.relay.port:61613}")
    private int relayPort;

    @Value("${spring.websocket.stomp.relay.client-login:myuser}")
    private String relayLogin;

    @Value("${spring.websocket.stomp.relay.client-passcode:mypassword}")
    private String relayPasscode;

    @Bean
    public TaskScheduler chessWebSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8); // Increased pool size for better handling
        scheduler.setThreadNamePrefix("chess-websocket-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30); // Increased termination time
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chess-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(20000) // Reduced to 20 seconds for better connection detection
                .setDisconnectDelay(3000); // Reduced to 3 seconds for faster cleanup
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

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        log.info("STOMP session connecting: {}", sessionId);
                        // Initialize session attributes
                        accessor.getSessionAttributes().put("CONNECTED_TIME", System.currentTimeMillis());
                        accessor.getSessionAttributes().put("ACTIVE", true);

                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        log.info("STOMP session disconnecting: {}", sessionId);
                        // Mark session as disconnecting to prevent message sending
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
                        // Final cleanup
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

        // Enhanced thread pool configuration
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
                    // Check if session is still active before sending
                    Boolean disconnecting = (Boolean) accessor.getSessionAttributes().get("DISCONNECTING");
                    Boolean completed = (Boolean) accessor.getSessionAttributes().get("COMPLETED");

                    if (Boolean.TRUE.equals(disconnecting) || Boolean.TRUE.equals(completed)) {
                        log.debug("Blocking message to disconnecting/completed session: {}",
                                accessor.getSessionId());
                        return null; // Block the message
                    }
                }
                return message;
            }
        });

        // Enhanced thread pool configuration for outbound
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(20)
                .queueCapacity(200)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(256 * 1024) // Increased to 256KB
                .setSendTimeLimit(20 * 1000) // Reduced to 20 seconds
                .setSendBufferSizeLimit(2 * 512 * 1024) // 1MB buffer
                .setTimeToFirstMessage(30 * 1000); // Reduced to 30 seconds
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        StompBrokerRelayRegistration relay = config.enableStompBrokerRelay("/topic","/queue","/exchange")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(relayLogin)
                .setClientPasscode(relayPasscode)
                .setSystemLogin(relayLogin)
                .setSystemPasscode(relayPasscode)
                .setVirtualHost("/")
                .setSystemHeartbeatSendInterval(10000)
                .setSystemHeartbeatReceiveInterval(10000)
                .setTaskScheduler(chessWebSocketTaskScheduler());

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}