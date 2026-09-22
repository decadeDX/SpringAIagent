package io.github.decadedx.springaiagent.service;

/**
 * 模型尝试第九次工具调用时中断，防止无限循环或成本失控。
 */
public class ToolLimitReachedException extends RuntimeException {
}
