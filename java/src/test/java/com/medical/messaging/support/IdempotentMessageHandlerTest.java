package com.medical.messaging.support;

import com.medical.common.RedisCacheUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentMessageHandlerTest {

    @Mock
    private RedisCacheUtil redisCacheUtil;

    @InjectMocks
    private IdempotentMessageHandler idempotentMessageHandler;

    @Test
    void tryAcquireResult_firstTimeReturnsAcquired() {
        when(redisCacheUtil.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(true);
        assertEquals(IdempotentMessageHandler.AcquireResult.ACQUIRED,
                idempotentMessageHandler.tryAcquireResult("EVT-1"));
    }

    @Test
    void tryAcquireResult_duplicateReturnsAlreadyProcessed() {
        when(redisCacheUtil.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(false);
        assertEquals(IdempotentMessageHandler.AcquireResult.ALREADY_PROCESSED,
                idempotentMessageHandler.tryAcquireResult("EVT-1"));
    }

    @Test
    void tryAcquireResult_redisErrorReturnsUnavailable() {
        when(redisCacheUtil.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(null);
        assertEquals(IdempotentMessageHandler.AcquireResult.REDIS_UNAVAILABLE,
                idempotentMessageHandler.tryAcquireResult("EVT-1"));
    }

    @Test
    void release_deletesProcessedKey() {
        idempotentMessageHandler.release("EVT-1");
        verify(redisCacheUtil).delete("mq:processed:EVT-1");
    }

    @Test
    @SuppressWarnings("deprecation")
    void tryAcquire_legacyWrapper() {
        when(redisCacheUtil.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(true);
        assertTrue(idempotentMessageHandler.tryAcquire("EVT-1"));
    }
}
