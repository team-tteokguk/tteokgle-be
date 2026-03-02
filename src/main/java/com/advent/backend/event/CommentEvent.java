package com.advent.backend.event;

import java.util.UUID;

public record CommentEvent(String commenterNickname, UUID ownerId, UUID storeId) {}
