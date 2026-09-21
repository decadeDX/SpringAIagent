package io.github.decadedx.springaiagent.enums;

/**
 * 预约事实的状态；取消后保留记录并释放对应时隙。
 */
public enum ReservationStatus {
    /** 已成功占用实验室时隙的有效预约。 */
    CONFIRMED,
    /** 已取消且已释放时隙的历史预约。 */
    CANCELLED
}
