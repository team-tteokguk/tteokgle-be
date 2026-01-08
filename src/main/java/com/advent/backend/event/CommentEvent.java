package com.advent.backend.event;

import com.advent.backend.entity.Member;
import javax.xml.stream.events.Comment;

public record CommentEvent(Member commenter, Member owner, Comment comment) {}
