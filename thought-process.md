# Backend Decisions & Trade-offs

## Read Performance vs Storage

Checking if a parent is free by joining bookings, offerings, and sessions is way too slow under concurrent load. I traded a tiny bit of write storage to completely optimize read speeds. I built a flattened helper table called booking_session_lock. The second a booking transaction hits, the app copies and flattens those specific session times right under the parent_id. Now conflict checks are a lightning-fast single table lookup with a simple query:

```sql
SELECT 1 FROM booking_session_lock
WHERE parent_id = :parentId
  AND start_time < :newEndTime
  AND end_time > :newStartTime

```

## Schema Layout

* **User Profiles**: Teacher and Parent tables hold a simple timezone string (like Asia/Kolkata). The Java app uses this boundary to shift internal UTC database times into localized views on the screen.
* **Course vs Offering**: Course is just a static template (Math 101) with no concept of time or staff. Offering is the live cohort that binds a course to a teacher and manages seat inventory limits (max_students, current_enrollment).
* **Sessions & Bookings**: Sessions are the literal calendar slots stored as TIMESTAMPTZ so Postgres forces UTC internally. Bookings act as the final transaction ledger.

## Concurrency and Timezones

I explicitly chose Pessimistic Locking (FOR UPDATE) over Optimistic version tracking for seat allocations because the user experience is significantly better when a hot class drops.

With optimistic locking, if 50 parents click the last seat at once, everyone gets to fill out the form, but 48 will hit a brutal database failure at the final checkout click. They have to refresh and do it all over again for a class that is already full. Pessimistic locking creates an orderly line the millisecond checkout starts. The winners get the spots, and the other 48 inbound requests wait a split second and immediately see a clean "Class is Full" error without wasting time filling out forms.

For timezones, nothing local touches the database. It stays 100% UTC. Shifting happens only at the API boundary when mapping Instant to ZonedDateTime. Spring Boot's Jackson mapper normally forces everything back to UTC strings, so I disabled that by setting write-dates-with-context-time-zone to false in application.yaml so the client actually receives the proper timezone offset (like +05:30).

## Code Structure Choices

* **OpenAPI Decoupling**: Swagger annotations make controllers incredibly messy. I moved all routing paths, query parameters, and OpenAPI definitions into separate Java interfaces (like TeacherApi) in a dedicated package. The actual controllers just implement those interfaces, keeping the core code clean and readable.
* **No Redundant Interfaces**: I didn't write useless interface pairs for my service layer (like BookingService to BookingServiceImpl). There is only one execution strategy here, and modern Spring Boot handles concrete classes seamlessly via CGLIB proxies. It removes the friction of updating two files for every single signature change.
* **Static Mappers**: I used simple utility classes with static methods (CourseMapper) for DTO conversions. It keeps mapping logic separate from business rules, compiles instantly, and avoids heavy runtime reflection or annotations from tools like MapStruct.
* **Java 21 Records**: All request and response DTOs use native records. They are immutable, thread-safe, and completely replace Lombok boilerplate.
* **Standard Errors**: I used Spring Boot 3's native ProblemDetail (RFC 7807) inside the global exception handler instead of a custom error wrapper. It projects validation errors in a standard web format out of the box.

## ID Allocation

I use application-driven UUID.randomUUID() in the service layer as the main strategy. It lets the app know the booking ID immediately so it can build and write the dependent booking_session_lock rows in the exact same database flow without waiting for a database return. The schema still retains a database fallback (DEFAULT gen_random_uuid()) so manual SQL inputs remain safe, and a hard UNIQUE(parent_id, session_id) constraint protects the lock table from race conditions.

## Retry-Safe Idempotency

If a network drop happens right after a parent pays, the client app retries. Without an idempotency layer, this double-books them or throws a messy constraint error.

I built a custom @Idempotent annotation backed by an AOP aspect (IdempotencyAspect) and an idempotent_request database table.

First, the aspect tries to write the incoming key to the database as PENDING using an isolated transaction (REQUIRES_NEW). If a parent spams the submit button, the second thread hits a unique key violation, stops right there, and returns a 409 Conflict. If the core service completes successfully, the aspect updates the row status to SUCCESS and caches the serialized JSON response. Subsequent retries with that key completely bypass the service logic and return the cached payload instantly. If the service throws a regular business exception, the aspect purges the key so the user can safely fix their input and retry.

To keep the database lean, a lightweight background cron job runs every hour to drop tracking keys older than 24 hours. I used PostgreSQL instead of Redis here to avoid adding extra infrastructure overhead and to guarantee persistent durability.

### Placement of @Idempotent on Implementation Classes (CGLIB Proxying Gotcha)
We explicitly place the `@Idempotent` annotation on the concrete controller classes (e.g. `ParentController`) rather than their OpenAPI interfaces (e.g. `ParentApi`). 

Under Spring Boot's default CGLIB class-based proxying, Spring AOP pointcuts (using `@annotation`) inspect the concrete target class methods at runtime. Because Java does not inherit annotations from interface methods onto implementing class methods, putting the annotation only on the interface would cause Spring AOP to silently bypass the aspect, failing to enforce idempotency. Placing it on the concrete class ensures it is intercepted correctly.