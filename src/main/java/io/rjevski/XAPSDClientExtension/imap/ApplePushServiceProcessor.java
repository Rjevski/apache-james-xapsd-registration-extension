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

package io.rjevski.XAPSDClientExtension.imap;

import com.google.common.collect.ImmutableList;
import io.rjevski.XAPSDClientExtension.xapsd.Client;
import org.apache.james.imap.api.ImapConfiguration;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.Capability;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.processor.CapabilityImplementingProcessor;
import org.apache.james.imap.processor.base.AbstractProcessor;
import org.apache.james.util.MDCBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.Properties;

/**
 * Processor for the XAPPLEPUSHSERVICE command, will relay between the IMAP client and xapsd.
 */
public class ApplePushServiceProcessor extends AbstractProcessor<ApplePushServiceImapPackages.ApplePushServiceRequest> implements CapabilityImplementingProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplePushServiceProcessor.class);

    private final StatusResponseFactory factory;
    private Client client;

    @Inject
    public ApplePushServiceProcessor(StatusResponseFactory factory) {
        super(ApplePushServiceImapPackages.ApplePushServiceRequest.class);
        this.factory = factory;
    }

    @Override
    public List<Capability> getImplementedCapabilities(ImapSession session) {
        return ImmutableList.of(Capability.of(ApplePushServiceImapPackages.APPLE_PUSH_SERVICE_COMMAND.getName()));
    }

    @Override
    public void configure(ImapConfiguration imapConfiguration) {
        Properties customProperties = imapConfiguration.getCustomProperties();

        String baseUrl = (String) customProperties.getOrDefault(
                "xapsd.baseUrl",
                // default port as per XAPSD config
                "http://localhost:11619/"
        );

        this.client = new Client(baseUrl);

        LOGGER.info(
                "{} initialized with base URL {} - change customProperties \"xapsd.baseUrl\" to override",
                this.getClass().getName(),
                baseUrl
        );
    }

    /**
     * Resolves the username.
     * This can be an arbitrary value but must match what the listener will resolve when handling mailbox events.
     */
    private String resolveUserName(ImapSession session) {
        return session.getUserName().asString();
    }

    /**
     * Resolve mailbox names from user-facing names.
     * Can be mapped to arbitrary values, but must match what the listener will resolve when handling mailbox events.
     * <p>
     * TODO: account for mailbox delegation,
     *   since user-visible IMAP names may vary between delegates of the same mailbox.
     *   This might require changes to xapsd, or even get rid of xapsd and implement its logic locally.
     *   For now, this is not an issue as xapsd doesn't yet know how to notify for non-INBOX mailboxes.
     */
    private String[] resolveMailboxNames(ImapSession session, String[] userFacingMailboxNames) {
        return userFacingMailboxNames;
    }

    @Override
    protected Mono<Void> doProcess(ApplePushServiceImapPackages.ApplePushServiceRequest request, Responder responder, ImapSession session) {
        String[] mailboxes = request.mailboxes;

        LOGGER.info(
                "Processing push request {}, {}, {}, {}",
                request.account_id,
                request.device_token,
                request.subtopic,
                String.join(", ", mailboxes)
        );

        if (mailboxes.length == 0) {
            LOGGER.warn(
                    "Mailboxes is empty, overriding to INBOX as per Dovecot code"
            );

            // Dovecot code makes this default to INBOX if not set?
            mailboxes = new String[]{"INBOX"};
        }

        return this.client.register(
                request.account_id,
                request.device_token,
                request.subtopic,
                resolveUserName(session),
                resolveMailboxNames(session, mailboxes)
        ).doOnSuccess(
                (topic) -> ok(request, responder, topic)
        ).doOnError(
                e -> fail(request, responder, HumanReadableText.FAILED)
        ).then();
    }

    /**
     * Returns a successful response.
     *
     * @param topic the APNS topic as returned by xapsd.
     */
    protected void ok(ApplePushServiceImapPackages.ApplePushServiceRequest request, ImapProcessor.Responder responder, String topic) {
        LOGGER.info(
                "Responding successfully with version {}, topic {}",
                request.version,
                topic
        );

        responder.respond(new ApplePushServiceImapPackages.ApplePushServiceResponse(request.version, topic));

        responder.respond(factory.taggedOk(request.getTag(), request.getCommand(), HumanReadableText.COMPLETED));
    }

    /**
     * Returns a failure response.
     *
     * @param text error message
     */
    protected void fail(ImapRequest request, ImapProcessor.Responder responder, HumanReadableText text) {
        LOGGER.error(
                "Responding with failure {}",
                text.asString()
        );

        responder.respond(factory.taggedBad(request.getTag(), request.getCommand(), text));
    }

    @Override
    protected MDCBuilder mdc(ApplePushServiceImapPackages.ApplePushServiceRequest message) {
        return MDCBuilder.create().addToContext(MDCBuilder.ACTION, ApplePushServiceImapPackages.APPLE_PUSH_SERVICE_COMMAND.getName()).addToContext("account_id", message.account_id).addToContext("device_token", message.device_token).addToContext("subtopic", message.subtopic).addToContext("mailboxes", String.join(",", message.mailboxes));
    }
}