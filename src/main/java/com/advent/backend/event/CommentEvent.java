package com.advent.backend.event;

import com.advent.backend.entity.GuestBook;
import com.advent.backend.entity.Member;

public record CommentEvent(Member commenter, Member owner, GuestBook guestBook) {}
