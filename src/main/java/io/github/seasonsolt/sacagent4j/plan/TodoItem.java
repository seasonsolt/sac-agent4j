package io.github.seasonsolt.sacagent4j.plan;

/** One plan item visible to the model in subsequent turns. */
public record TodoItem(int id, String content, TodoStatus status) {}
