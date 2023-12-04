package io.rjevski.XAPSDClientExtension.xapsd;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the payload to xapsd's /register endpoint.
 */
public class RegisterRequest {
    @JsonProperty("ApsAccountId")
    public final String account_id;

    @JsonProperty("ApsDeviceToken")
    public final String device_token;

    @JsonProperty("ApsSubtopic")
    public final String subtopic;

    @JsonProperty("Username")
    public final String username;

    @JsonProperty("Mailboxes")
    public final String[] mailboxes;

    public RegisterRequest(
            String account_id,
            String device_token,
            String subtopic,
            String username,
            String[] mailboxes
    ) {
        this.account_id = account_id;
        this.device_token = device_token;
        this.subtopic = subtopic;
        this.username = username;
        this.mailboxes = mailboxes;
    }
}
