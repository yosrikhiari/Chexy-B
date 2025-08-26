package org.example.chessmystic.Config.websocket;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;

public class RabbitMQConfig {

    public static final String CHESS_EXCHANGE = "chess.exchange";
    public static final String CHESS_DEAD_LETTER_EXCHANGE = "chess.dlx";


    public static final String GAME_UPDATE_QUEUE = "game.update";
    public static final String MATCHMAKING_QUEUE = "matchmaking.queue";
    public static final String PLAYER_MOVES = "player.moves";
    public static final String TIMER_UPDATE = "timer.update";

    public static final String GAME_UPDATE_RK = "game.update.rk"; // RK stands for routing key
    public static final String MATCHMAKING_RK = "matchmaking.rk";
    public static final String PLAYER_MOVES_RK = "player.moves.rk";
    public static final String TIMER_UPDATE_RK = "timer.update.rk";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setMessageConverter(jsonMessageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("CorrelationData: " + correlationData);
            }
            else{
                System.out.println("Problem: " + cause);
            }
        });
        return template;
    }


    // the part that does all the dirty work be it establishing connection/channels/ and managing concurrency-threads-errors ...
    @Bean
    public SimpleRabbitListenerContainerFactory  messageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean
    public TopicExchange chessExchange() {
        return new TopicExchange(CHESS_EXCHANGE);
    }

    @Bean
    public TopicExchange DeadLetterExchange() {
        return new TopicExchange(CHESS_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue matchmakingQueue() {
        return QueueBuilder.durable(MATCHMAKING_QUEUE)
                .withArgument("x-dead-letters-exchange", CHESS_DEAD_LETTER_EXCHANGE)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    @Bean
    public Queue playerMoveQueue() {
        return QueueBuilder.durable(PLAYER_MOVES)
                .withArgument("x-dead-letters-exchange", CHESS_DEAD_LETTER_EXCHANGE)
                .withArgument("x-message-ttl", 60000)
                .build();
    }

    @Bean
    public Queue timerUpdatesQueue() {
        return QueueBuilder.durable(TIMER_UPDATE)
                .withArgument("x-dead-letters-exchange", CHESS_DEAD_LETTER_EXCHANGE)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue gameUpdatesQueue() {
        return QueueBuilder.durable(GAME_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", CHESS_DEAD_LETTER_EXCHANGE)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .build();
    }

    // The Bindings

    @Bean
    public Binding gameUpdatesBinding() {
        return BindingBuilder.bind(gameUpdatesQueue())
                .to(chessExchange())
                .with(GAME_UPDATE_RK);
    }
    @Bean
    public Binding playerMovesBinding() {
        return BindingBuilder.bind(playerMoveQueue())
                .to(chessExchange())
                .with(PLAYER_MOVES_RK);
    }
    @Bean
    public Binding timerUpdatesBinding() {
        return BindingBuilder.bind(timerUpdatesQueue())
                .to(chessExchange())
                .with(TIMER_UPDATE_RK);
    }
    @Bean
    public Binding matchmakingBinding() {
        return BindingBuilder.bind(matchmakingQueue())
                .to(chessExchange())
                .with(MATCHMAKING_RK);
    }

}
