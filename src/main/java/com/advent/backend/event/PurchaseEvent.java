package com.advent.backend.event;

import com.advent.backend.entity.Member;

public record PurchaseEvent(Member sender, Member receiver, String itemName) {}
