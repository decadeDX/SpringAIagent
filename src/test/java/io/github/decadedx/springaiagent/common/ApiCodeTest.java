package io.github.decadedx.springaiagent.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiCodeTest {

    @Test
    void shouldExposeOnlyUniqueIntegerCodes() {
        long distinctCount = java.util.Arrays.stream(ApiCode.values())
                .map(ApiCode::value)
                .distinct()
                .count();

        assertEquals(ApiCode.values().length, distinctCount);
        assertEquals(200, ApiCode.OK.value());
        assertEquals(40100, ApiCode.UNAUTHENTICATED.value());
        assertEquals(40901, ApiCode.RESERVATION_CONFLICT.value());
        assertTrue(ApiCode.INTERNAL_ERROR.value() >= 50000);
    }
}
