package com.voum.modules.realtime.config;

import com.voum.modules.realtime.listeners.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisRealtimeConfig {

    public static final String REALTIME_CHANNEL = "voum:realtime:events";

    @Bean
    public ChannelTopic realtimeTopic() {
        return new ChannelTopic(REALTIME_CHANNEL);
    }

    @Bean
    public MessageListenerAdapter realtimeListenerAdapter(RedisMessageSubscriber subscriber) {
        // Wrap our subscriber; Jackson2JsonRedisSerializer is used within the subscriber for deserialization
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic topic) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, topic);
        return container;
    }
}
