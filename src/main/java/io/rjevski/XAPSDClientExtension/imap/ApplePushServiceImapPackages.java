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
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.Tag;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.message.request.AbstractImapRequest;
import org.apache.james.modules.protocols.ImapPackage;
import org.apache.james.utils.ClassName;

import javax.inject.Inject;

/**
 * Main entry point to the XAPPLEPUSHSERVICE IMAP extension.
 */
public class ApplePushServiceImapPackages extends ImapPackage.Impl {
    public static ImapCommand APPLE_PUSH_SERVICE_COMMAND = ImapCommand.authenticatedStateCommand("XAPPLEPUSHSERVICE");

    @Inject
    public ApplePushServiceImapPackages() {
        super(ImmutableList.of(new ClassName(ApplePushServiceProcessor.class.getCanonicalName())),
                ImmutableList.of(new ClassName(ApplePushServiceCommandParser.class.getCanonicalName())),
                ImmutableList.of(new ClassName(ApplePushServiceResponseEncoder.class.getCanonicalName()))
        );
    }

    /**
     * Represents the XAPPLEPUSHSERVICE request/command data.
     */
    public static class ApplePushServiceRequest extends AbstractImapRequest {
        /**
         * The version as provided by the client. We store this as we must return it within the response.
         * We currently validate this against a single hardcoded value but in the future could support multiple versions.
         */
        public final String version;

        // the below are as provided by the iOS device
        public final String account_id;
        public final String device_token;
        public final String subtopic;
        public final String[] mailboxes;

        public ApplePushServiceRequest(Tag tag, String version, String account_id, String device_token, String subtopic, String[] mailboxes) {
            super(tag, APPLE_PUSH_SERVICE_COMMAND);

            this.version = version;
            this.account_id = account_id;
            this.device_token = device_token;
            this.subtopic = subtopic;
            this.mailboxes = mailboxes;
        }
    }

    /**
     * Represents the XAPPLEPUSHSERVICE response's data.
     */
    public static class ApplePushServiceResponse implements ImapResponseMessage {
        /**
         * Note: must match the request's version.
         */
        public final String version;
        /**
         * APNS topic to listen on - returned by xapsd.
         */
        public final String topic;

        public ApplePushServiceResponse(String version, String topic) {
            this.version = version;
            this.topic = topic;
        }
    }
}
