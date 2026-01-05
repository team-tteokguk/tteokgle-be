package com.advent.backend.event;

import com.advent.backend.entity.Member;
import java.util.UUID;

public record TransferEvent(UUID txId, Member sender, Member receiver, String itemName) {}
