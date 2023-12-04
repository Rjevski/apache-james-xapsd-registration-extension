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

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.Tag;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses an XAPPLEPUSHSERVICE IMAP command.
 */
public class ApplePushServiceCommandParser extends AbstractImapCommandParser {
    /**
     * For now, we expect and understand version 2 only.
     */
    public final String expected_version = "2";

    @Inject
    public ApplePushServiceCommandParser(StatusResponseFactory statusResponseFactory) {
        super(ApplePushServiceImapPackages.APPLE_PUSH_SERVICE_COMMAND, statusResponseFactory);
    }

    /**
     * "Consumes" an "atom" object off the request and validates it against the provided value, throwing if no match.
     *
     * @throws DecodingException if it doesn't validate or if the underlying Reader throws
     */
    private static void consumeAtomAndVerify(ImapRequestLineReader request, String expected) throws DecodingException {
        String actual = request.atom();

        if (!Objects.equals(actual, expected)) {
            throw new DecodingException(
                    HumanReadableText.ILLEGAL_ARGUMENTS, String.format("Expected %s, got %s.", expected, actual)
            );
        }
    }

    /**
     * Consumes a "(INBOX Sent Drafts Etc)" structure, returning the items as an array.
     *
     * @return array of collected mailbox names
     * @throws DecodingException if the underlying reader throws
     */
    private static String[] consumeMailBoxes(ImapRequestLineReader request) throws DecodingException {
        List<String> mailboxes = new ArrayList<>();

        request.nextWordChar();
        request.consumeChar('(');
        request.nextWordChar();

        while (request.nextChar() != ')') {
            mailboxes.add(request.mailbox());
            request.nextWordChar();
        }
        request.consumeChar(')');

        return mailboxes.toArray(new String[0]);
    }


    @Override
    protected ImapMessage decode(ImapRequestLineReader request, Tag tag, ImapSession session) throws DecodingException {
        consumeAtomAndVerify(request, "aps-version");

        String version = request.astring();

        if (!Objects.equals(version, expected_version)) {
            throw new DecodingException(
                    HumanReadableText.ILLEGAL_ARGUMENTS,
                    String.format("Unknown aps-version %s, expected %s.", version, expected_version)
            );
        }

        consumeAtomAndVerify(request, "aps-account-id");
        final String account_id = request.astring();

        consumeAtomAndVerify(request, "aps-device-token");
        final String device_token = request.astring();

        consumeAtomAndVerify(request, "aps-subtopic");
        final String subtopic = request.astring();

        consumeAtomAndVerify(request, "mailboxes");
        final String[] mailboxes = consumeMailBoxes(request);

        // note that we intentionally do not validate whether any mailboxes have been provided or if they are valid
        // the original Dovecot plugin assumes "INBOX" if no explicit mailboxes were passed; we do this in the Processor

        request.eol();

        return new ApplePushServiceImapPackages.ApplePushServiceRequest(
                tag,
                version,
                account_id,
                device_token,
                subtopic,
                mailboxes
        );
    }
}