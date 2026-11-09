package com.aar.common.state;

public record Transition(EntityType entity, String from, String event, String to) {
}
