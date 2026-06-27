package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.JsonNode;

/** One non-header entry in a JSONL session tree. */
public record SessionEntry(String id, String parentId, String type, JsonNode node) {}
