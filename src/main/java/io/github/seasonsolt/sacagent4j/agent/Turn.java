package io.github.seasonsolt.sacagent4j.agent;

/** One trajectory step: the model chose an action, then the tool returned an observation. */
public record Turn(Action action, Observation observation) {}
