package com.medical.messaging.config;

import com.medical.messaging.appointment.AppointmentEventPublisher;
import com.medical.messaging.appointment.NoOpAppointmentEventPublisher;
import com.medical.messaging.appointment.RabbitAppointmentEventPublisher;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {

    /**
     * messaging 开启：真实 Rabbit 发布 + 启用 {@code @RabbitListener} 扫描。
     */
    @Configuration
    @ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "true")
    @EnableRabbit
    static class EnabledMessagingConfiguration {

        @Bean
        public AppointmentEventPublisher rabbitAppointmentEventPublisher(RabbitTemplate rabbitTemplate,
                                                                         MessagingProperties messagingProperties) {
            return new RabbitAppointmentEventPublisher(rabbitTemplate, messagingProperties);
        }
    }

    /** messaging 关闭或未配置：注入 NoOp，业务层仍可注入 Bridge 而不报错 */
    @Bean
    @ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "false", matchIfMissing = true)
    public AppointmentEventPublisher noOpAppointmentEventPublisher() {
        return new NoOpAppointmentEventPublisher();
    }
}
