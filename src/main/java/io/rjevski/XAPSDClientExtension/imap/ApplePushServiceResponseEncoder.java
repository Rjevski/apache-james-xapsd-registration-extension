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

import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.ImapResponseEncoder;

import java.io.IOException;

/**
 * Encodes a response to the XAPPLEPUSHSERVICE command.
 */
public class ApplePushServiceResponseEncoder implements ImapResponseEncoder<ApplePushServiceImapPackages.ApplePushServiceResponse> {

    @Override
    public Class<ApplePushServiceImapPackages.ApplePushServiceResponse> acceptableMessages() {
        return ApplePushServiceImapPackages.ApplePushServiceResponse.class;
    }

    @Override
    public void encode(ApplePushServiceImapPackages.ApplePushServiceResponse message, ImapResponseComposer composer) throws IOException {
        composer.untagged();

        composer.commandName(ApplePushServiceImapPackages.APPLE_PUSH_SERVICE_COMMAND);

        composer.message("aps-version");
        composer.quote(message.version);

        composer.message("aps-topic");
        composer.quote(message.topic);

        composer.end();
    }
}
