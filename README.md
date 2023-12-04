# Apache James XAPSD extension

This Apache James extension allows the mail server to respond to `XAPPLEPUSHSERVICE` IMAP commands and forward them to [dovecot-xaps-daemon](https://github.com/freswa/dovecot-xaps-daemon), effectively enabling IMAP push email on iOS devices. This is heavily based on the example [IMAP extensions](https://github.com/apache/james-project/tree/master/examples/custom-imap) and [listeners](https://github.com/apache/james-project/tree/master/examples/custom-listeners).

This is structured in 2 parts:

## IMAP extension

An IMAP extension is used to handle the `XAPPLEPUSHSERVICE` IMAP command sent by iOS devices. This command is sent periodically to inform the mail server of the device's APNS tokens and the mailboxes it would like to be notified about. We forward this command's data to xapsd which updates its local database.

## Mailbox listener

A mailbox listener is used to listen to relevant mailbox events (such as new mail) and forward them onto xapsd which will, after consulting its local database for the APNS token corresponding to the given username & mailbox, will send a notification to the device.

# Installation

* set up [dovecot-xaps-daemon](https://github.com/freswa/dovecot-xaps-daemon)
* put the JAR into James' extension jars directory
* add the following to their respective files

`imapserver.xml`:

```xml
<imapPackages>org.apache.james.modules.protocols.DefaultImapPackage</imapPackages>
<imapPackages>io.rjevski.XAPSDClientExtension.imap.ApplePushServiceImapPackages</imapPackages>
<customProperties>xapsd.baseUrl=https://xapsd.example.com/</customProperties>
```

(the `xapsd.baseUrl` custom property controls the base URL of xapsd *for the IMAP extension only*)

`listeners.xml`:

```xml
<listeners>
    <executeGroupListeners>true</executeGroupListeners>
    <listener>
        <class>io.rjevski.XAPSDClientExtension.listener.ApplePushServiceMailboxListener</class>
        <group>XAPSDClientExtension-group</group>
        <configuration>
            <xapsd.baseUrl>https://xapsd.example.com/</xapsd.baseUrl>
        </configuration>
        <async>true</async>
    </listener>
</listeners>
```

More info on listener configuration is available [on the official website](https://james.apache.org/howTo/custom-listeners.html).

# TODO:

* unit tests
* unified configuration value - currently config is separate between IMAP extension & listener
* release precompiled binaries
* embed XAPSD functionality into James itself, removing the need for a separate service
* reverse-engineer how to notify for non-INBOX folders, throwing `MobileMail.app` into a disassembler should do it

# License

Apache 2.0, see `LICENSE` for details. It is the same as Apache James and the file is copied straight from its repo.
