package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import org.junit.Test;

import static org.junit.Assert.*;

public class ITReadAllEventsBackwardWithLinkToDeletedEvents extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void readsOneEvent() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(), newTestEvent()).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(), newLinkEvent(deletedStreamName, 0)).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.any()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(linkedStreamName, 0, 1, true).join();

        assertEquals(1, slice.events.size());

        ResolvedEvent resolvedEvent = slice.events.get(0);
        assertNull("Linked event was resolved", resolvedEvent.event);
        assertNotNull("Linked event is not included", resolvedEvent.originalEvent());
        assertFalse("Event was resolved", resolvedEvent.isResolved());
    }

    private static EventData newLinkEvent(String stream, int eventNumber) {
        return EventData.newBuilder()
            .type(SystemEventType.LINK_TO.value)
            .data(eventNumber + "@" + stream)
            .build();
    }

}
