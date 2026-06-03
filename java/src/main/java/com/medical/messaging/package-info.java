/**
 * 预约领域事件与 RabbitMQ 集成。
 *
 * <p><b>整体链路</b>（业务 Service 不直接依赖 RabbitTemplate）：
 * <pre>
 *   AppointmentServiceImpl / Scheduler
 *        → AppointmentEventBridge（发 Spring 领域事件）
 *        → AppointmentEventAfterCommitListener（事务 commit 后发 MQ）
 *        → Topic Exchange {@code medical.appointment.topic}
 *        → 三条队列：notification / slot-restore / audit
 * </pre>
 *
 * <p><b>设计要点</b>
 * <ul>
 *   <li>副作用（通知、过期回补号源、审计）与预约主事务解耦</li>
 *   <li>消费端 Redis 幂等 + 失败 release，配合手动 ack / DLQ</li>
 *   <li>{@code medical.messaging.enabled=false} 时降级 NoOp，便于单测与无 MQ 环境</li>
 * </ul>
 *
 * <p>拓扑常量见 {@link com.medical.messaging.config.RabbitMqTopology}；
 * 本地启动 RabbitMQ：{@code docker compose -f infra/docker-compose.yml up -d rabbitmq}
 */
package com.medical.messaging;
