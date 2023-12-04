package io.rjevski.XAPSDClientExtension.xapsd;


import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * Client for <a href="https://github.com/freswa/dovecot-xaps-daemon">xapsd</a>'s HTTP API.
 */
public class Client {
    private final WebClient client;

    public Client(String baseUrl) {
        this.client = WebClient.create(baseUrl);
    }

    /**
     * Registers a given account/device/subtopic/username
     *
     * @param account_id   mail account reference as sent by the iOS device
     * @param device_token token as sent by the iOS device
     * @param subtopic     APNS subtopic as sent by the iOS device, will be stored by xapsd and used during notify
     * @param username     username of the client - can be arbitrary but must use the same one in notify
     * @param mailboxes    list of mailbox names - can be arbitrary but must use the same ones when calling notify
     *                     Note: xapsd doesn't yet know how to notify for anything other than INBOX
     * @return the root topic, to return in the IMAP response.
     */
    public Mono<String> register(String account_id, String device_token, String subtopic, String username, String[] mailboxes) {
        RegisterRequest request = new RegisterRequest(account_id, device_token, subtopic, username, mailboxes);

        return this.client.post().uri("/register").contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(request)).retrieve().bodyToMono(String.class);
    }

    /**
     * Notify of a mailbox event.
     * xapsd will look up the account ID/device token/subtopic to notify
     * based on the username and mailbox names provided, thus they must match what was sent during register.
     *
     * @param username username of the client - can be arbitrary but must use the same one in notify
     * @param mailbox  mailbox name - can be arbitrary but must use the same ones when calling notify
     *                 Note: xapsd doesn't yet know how to notify for anything other than INBOX
     * @param events   list of mailbox events, looks like <a href="https://www.rfc-editor.org/rfc/rfc5423.html">RFC5423</a>?
     * @return Mono void.
     */
    public Mono<Void> notifyOfEvents(String username,
                                     String mailbox,
                                     String[] events) {
        NotifyRequest request = new NotifyRequest(username, mailbox, events);

        return this.client.post().uri("/notify").contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(request)).retrieve().bodyToMono(Void.class);
    }
}
