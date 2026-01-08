package com.advent.backend.event;

import com.advent.backend.entity.Member;

public record SubscribeEvent(Member subscriber, Member target) {}
