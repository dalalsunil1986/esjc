package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.AccessDeniedException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ITCreatePersistentSubscription extends AbstractEventStoreTest {

    public ITCreatePersistentSubscription(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void createsPersistentSubscriptionOnExistingStream() {
        final String stream = generateStreamName();
        final String group = "existing";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        eventstore.createPersistentSubscription(stream, group, settings).join();
    }

    @Test
    public void createsPersistentSubscriptionOnNonExistingStream() {
        final String stream = generateStreamName();
        final String group = "nonexistinggroup";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();
    }

    @Test
    public void failsToCreateDuplicatePersistentSubscriptionGroup() {
        final String stream = generateStreamName();
        final String group = "group32";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        try {
            eventstore.createPersistentSubscription(stream, group, settings).join();
            fail("should fail with 'IllegalStateException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(IllegalStateException.class));
        }
    }

    @Test
    public void createsDuplicatePersistentSubscriptionGroupNameOnDifferentStreams() {
        final String stream = generateStreamName();
        final String group = "group3211";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.createPersistentSubscription("other-" + stream, group, settings).join();
    }

    @Test
    public void failsToCreatePersistentSubscriptionGroupWithoutPermissions() {
        final String stream = "$" + generateStreamName();
        final String group = "group57";

        EventStore unauthenticatedEventstore = EventStoreBuilder.newBuilder(eventstore.settings())
            .noUserCredentials()
            .defaultExecutor()
            .build();

        try {
            PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
                .resolveLinkTos(false)
                .startFromCurrent()
                .build();

            try {
                unauthenticatedEventstore.createPersistentSubscription(stream, group, settings).join();
                fail("should fail with 'AccessDeniedException'");
            } catch (Exception e) {
                assertThat(e.getCause(), instanceOf(AccessDeniedException.class));
            }
        } finally {
            unauthenticatedEventstore.shutdown();
        }
    }

    @Test
    public void createsPersistentSubscriptionAfterDeletingTheSame() {
        final String stream = generateStreamName();
        final String group = "existing";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.deletePersistentSubscription(stream, group).join();

        eventstore.createPersistentSubscription(stream, group, settings).join();
    }

}
