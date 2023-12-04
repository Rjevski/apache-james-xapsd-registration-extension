/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package io.rjevski.XAPSDClientExtension.listener;

import io.rjevski.XAPSDClientExtension.xapsd.Client;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.james.events.Event;
import org.apache.james.events.EventListener;
import org.apache.james.events.Group;
import org.apache.james.mailbox.events.MailboxEvents;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Listens to relevant mailbox events and forwards them to xapsd.
 */
class ApplePushServiceMailboxListener implements EventListener.ReactiveGroupEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplePushServiceMailboxListener.class);
    private static final ApplePushServiceMailboxListenerGroup GROUP = new ApplePushServiceMailboxListenerGroup();
    private final Client client;
    @Inject
    ApplePushServiceMailboxListener(
            HierarchicalConfiguration<ImmutableNode> config
    ) {
        String baseUrl = config.getString(
                "xapsd.baseUrl",
                // default port as per XAPSD config
                "http://localhost:11619/"
        );

        this.client = new Client(
                baseUrl
        );

        LOGGER.info(
                "{} initialized with base URL {}, change listener config \"xapsd.baseUrl\" to override",
                this.getClass().getName(),
                baseUrl
        );
    }

    @Override
    public Group getDefaultGroup() {
        return GROUP;
    }

    @Override
    public Mono<Void> reactiveEvent(Event event) {
        LOGGER.debug(
                "Received event {}",
                event.getClass().getName()
        );

        if (event instanceof MailboxEvents.Added) {
            MailboxEvents.Added addedEvent = (MailboxEvents.Added) event;

            Set<String> mailboxEvents = new HashSet<>();

            // a single James event can represent multiple operations
            if (addedEvent.isDelivery()) {
                mailboxEvents.add("MessageNew");
            }

            if (addedEvent.isAppended() || addedEvent.isMoved()) {
                mailboxEvents.add("MessageAppend");
            }

            if (!mailboxEvents.isEmpty()) {
                return handle(
                        // TODO: is the username even set if it's a delivery instead of local append? since there is no IMAP session
                        resolveUserName(addedEvent),
                        resolveMailboxName(addedEvent),
                        mailboxEvents.toArray(new String[0])
                );
            }
        } else if (event instanceof MailboxEvents.Expunged) {
            MailboxEvents.Expunged expungedEvent = (MailboxEvents.Expunged) event;

            return handle(
                    resolveUserName(expungedEvent),
                    resolveMailboxName(expungedEvent),
                    new String[]{"MessageExpunge"}
            );
        } else if (event instanceof MailboxEvents.FlagsUpdated) {
            MailboxEvents.FlagsUpdated flagsEvent = (MailboxEvents.FlagsUpdated) event;

            Set<String> mailboxEvents = new HashSet<>();

            for (UpdatedFlags update : flagsEvent.getUpdatedFlags()) {
                if (update.flagsChanged()) {
                    // TODO: determine differences by diffing old/new flags and setting flagsSet/Removed accordingly
                    //  for now we always assume both operations happened
                    mailboxEvents.add("FlagsSet");
                    mailboxEvents.add("FlagsClear");
                }
            }

            if (!mailboxEvents.isEmpty()) {
                return handle(
                        resolveUserName(flagsEvent),
                        resolveMailboxName(flagsEvent),
                        mailboxEvents.toArray(new String[0])
                );
            }
        }
        // TODO: handle other events defined in https://datatracker.ietf.org/doc/html/rfc5423

        return Mono.empty();
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return ExecutionMode.ASYNCHRONOUS;
    }

    @Override
    public boolean isHandling(Event event) {
        return event instanceof MailboxEvents.Added || event instanceof MailboxEvents.Expunged || event instanceof MailboxEvents.FlagsUpdated;
    }

    /**
     * Resolves the username.
     * <p>
     * TODO: make sure this is the same username that originally called the XAPPLEPUSHSERVICE command
     *   this might be more complex than it looks when you account for aliases, mailbox delegation, etc
     *
     * @return the same value as resolved within the IMAP extension when registering.
     */

    private String resolveUserName(MailboxEvents.MessageEvent event) {
        return event.getUsername().asString();
    }

    /**
     * Resolves the mailbox name.
     * <p>
     * TODO: account for mailbox delegation,
     *   since user-visible IMAP names may vary between delegates of the same mailbox.
     *   This might require changes to xapsd, or even get rid of xapsd and implement its logic locally.
     *   For now, this is not an issue as xapsd doesn't yet know how to notify for non-INBOX mailboxes.
     *
     * @return the same value as resolved within the IMAP extension when registering.
     */
    private String resolveMailboxName(MailboxEvents.MessageEvent event) {
        return event.getMailboxPath().getName();
    }

    /**
     * Send the request to xapsd.
     */
    private Mono<Void> handle(
            String userName,
            String mailboxName,
            String[] eventTypes
    ) {
        LOGGER.info(
                "Sending events {} for username {}, mailbox {}",
                String.join(", ", eventTypes),
                userName,
                mailboxName
        );

        return this.client.notifyOfEvents(
                userName,
                mailboxName,
                eventTypes
        );
    }

    public static class ApplePushServiceMailboxListenerGroup extends Group {
    }
}
