package io.rjevski.XAPSDClientExtension.xapsd;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the payload to xapsd's /notify endpoint.
 */
public class NotifyRequest {
    @JsonProperty("Username")
    public final String username;

    @JsonProperty("Mailbox")
    public final String mailbox;

    @JsonProperty("Events")
    public final String[] events;

    public NotifyRequest(
            String username,
            String mailbox,
            String[] events
    ) {
        this.username = username;
        this.mailbox = mailbox;
        this.events = events;
    }
}
