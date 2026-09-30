# Organizational Communication System (OCS)

A web-based internal communication platform for organizations. Employees, managers and administrators
exchange feedback, complaints and requests (optionally anonymously), publish announcements and polls,
schedule meetings, share a calendar and report policy violations, all from one place.

The full requirements are in [`docs/SRS.pdf`](docs/SRS.pdf).

## Features

|Role|Capabilities|
|-|-|
|Employee|Register (pending approval), log in/out, send messages (feedback / complaint / request / general) with an anonymity option, inbox and outbox, personal login history, view announcements, polls, calendar and meetings, report issues|
|Manager|Everything an employee can do, plus publish announcements, create polls, schedule meetings (with conflict detection), add and delete calendar events|
|Administrator|Approve or reject registrations, suspend / deactivate / reactivate users, change roles, handle reported violations, view all login history and the administrator audit log|

Security highlights: salted PBKDF2-HMAC-SHA256 password hashing, role-based access control on every route,
session inactivity timeout, account lockout after repeated failed logins, HTML output escaping, prepared
statements everywhere, state-changing actions only via POST, and anonymous sender identity withheld at the
query level.

## Tech stack

* Java 17+ with the built-in `com.sun.net.httpserver` server (no web framework)
* MySQL 8+ through JDBC (`mysql-connector-j`)
* Maven for build and packaging
* Server-rendered HTML with a single stylesheet

## Getting started

1. **Prerequisites:** JDK 17+, Maven 3.8+, MySQL 8+.
2. **Create the database**

```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed.sql        # optional sample data
   ```

   Create a MySQL user for the app and grant it access to the `ocs` database.

3. **Configure**

```bash
   cp src/main/resources/config.properties.example config.properties
   ```

   Edit `config.properties` with your database credentials. The `admin.\*` values are used to create the
first administrator on first start, so change the default password.

4. **Run**

```bash
   mvn compile exec:java -Dexec.mainClass=com.ocs.Main
   ```

   or build a runnable jar:

```bash
   mvn clean package
   java -jar target/ocs.jar
   ```

5. Open [http://localhost:8080](http://localhost:8080) and log in with the administrator from `config.properties`.
Register other users from the home page, then approve them under **Users**.

To load the configuration from another location, pass `-Docs.config=/path/to/config.properties`.

## Project structure

```
src/main/java/com/ocs/
├── Main.java            Server start-up and URL routing
├── Database.java        Config loading and JDBC connections
├── model/               Plain data classes (User, Message, Poll, ...)
├── dao/                 SQL access, one DAO per table group
├── handler/             HTTP handlers, one per feature area (BaseHandler holds shared logic)
└── util/                HtmlUtil, FormParser, PasswordHasher, SessionManager
database/                schema.sql and seed.sql
docs/SRS.pdf             Software Requirements Specification
```

## Configuration reference

|Key|Purpose|Default|
|-|-|-|
|`db.url`, `db.user`, `db.password`|MySQL connection|none|
|`server.port`|HTTP port|8080|
|`cookie.secure`|Add `Secure` to the session cookie (enable behind HTTPS)|false|
|`session.timeout.minutes`|Inactivity timeout (SC-5)|30|
|`login.max.attempts`|Failed logins before lockout (SC-6)|5|
|`admin.name`, `admin.username`, `admin.password`|First administrator, created only if none exists|see example file|

## Known limitations

This is a version 1.0 academic implementation. Items from the SRS that are not implemented yet:
departmental policy management, automated e-mail/SMS/push notifications and reminders, message replies,
password recovery, WebSocket real-time updates, scheduled backups and data-retention archiving.
Serve the app behind an HTTPS reverse proxy (for example Nginx) in any real deployment, since the built-in
server speaks plain HTTP. Meeting conflict detection compares exact start times only.

## License

Add a license of your choice before publishing.

