package com.likelion.realtalk.debate.service;

import com.likelion.realtalk.debate.model.DebateRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebateEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishDebateStart(DebateRoom room) {
        eventPublisher.publishEvent(new DebateStartEvent(this, room));
    }

    public static class DebateStartEvent extends org.springframework.context.ApplicationEvent {
        private final DebateRoom room;

        public DebateStartEvent(Object source, DebateRoom room) {
            super(source);
            this.room = room;
        }

        public DebateRoom getRoom() {
            return room;
        }
    }
}
