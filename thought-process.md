## The Big Trade-Off

* **The Problem**: Checking if a parent is free usually means joining Bookings to Offerings, and then Offerings to Sessions. Doing that every time someone hits "Book" slows the database down to a crawl.
* **The Fix**: I added a helper table called `BOOKING_SESSION_LOCK`. The second a booking goes through, we copy and flatten those session times directly under the parent's ID. It uses a bit more storage, but it makes conflict checks a lightning-fast single table lookup.

---

## What Each Table Does

* `TEACHER` & `PARENT`: Basic user profiles. Both save a `timezone` string (like `Europe/London`). The Java backend uses this to shift UTC database times into their local time on the screen.
* **`COURSE`**: Just the static template for a class (like "Math 101"). It has no concept of time, teachers, or schedules.
* **`OFFERING`**: This is the live cohort connecting a course to a teacher. It manages the inventory using `max_students` and `current_enrollment`.
* **`SESSION`**: The actual calendar dates and times for an offering. Stored strictly as `TIMESTAMPTZ` so Postgres forces everything into UTC internally.
* **`BOOKING`**: The source of truth showing a parent successfully registered for an offering.
* **`BOOKING_SESSION_LOCK`**: The calendar blockout table. It maps a parent directly to their busy time slots so we can see their availability instantly.

---

## Concurrency & Timezones

* **Pessimistic vs Optimistic**: I deliberately chose **Pessimistic Locking** (`FOR UPDATE`) over Optimistic Locking because it gives a much better user experience when a highly popular class drops.
* **The UX Problem with Optimistic**: If 50 parents try to grab the last 2 spots at once, optimistic locking lets everyone click through, fills out their info, and hits buy. Then, 48 parents get a frustrating error message at the very last second telling them their transaction failed. They have to refresh, retry, and fill everything out again, only to find the class is full.
* **The UX Fix with Pessimistic**: Pessimistic locking creates an orderly line the second checkout starts. The first two parents get the spots. The other 48 requests wait for a split second, immediately see the class is full, and get a clear "Class is Full" message right away. Nobody wastes time filling out forms for a spot that is already gone.
* **Timezone Strategy**: Zero local times are saved in the DB. Everything stays strict UTC internally. Timezone shifting happens **only at the API boundary** when mapping UTC `Instant` to localized `ZonedDateTime` in response DTOs.
  * *Jackson Fix*: By default, Spring Boot's Jackson normalizes all `ZonedDateTime` responses back to UTC. I disabled this (`write-dates-with-context-time-zone: false` in `application.yaml`) so the parent actually sees the timezone offset (like `+05:30`) on their screen.

---

## Why It Holds Up Under Load

* **Quick Checks**: To see if a parent is double-booked, the code avoids heavy joins and looks at a single table:
```sql
SELECT 1 FROM booking_session_lock
WHERE parent_id = :parentId
  AND start_time < :newEndTime
  AND end_time > :newStartTime

```



* **Double Clicks**: I put a unique constraint on `BOOKING(parent_id, offering_id)`. This stops a parent from accidentally buying the exact same class twice if they spam the submit button.
* **Scope**: I only checked for parent schedule conflicts and class capacity limits here. Checking if a teacher is double-booked across different classes is left out to keep the project clean and focused.

---

## Clean Code & Clean APIs

* **Swagger Clutter & Package Separation**: OpenAPI/Swagger docs make controller classes super messy and hard to read. To fix this, I put all Swagger and Spring MVC mapping annotations inside separate Java interfaces (like `TeacherApi`) in their own package (`com.undoschool.booking.api`). The actual controllers (in `com.undoschool.booking.controller`) just implement those interfaces. This keeps the controllers 100% clean and keeps the packages neatly organized.
* **Concrete Service Classes**: I chose not to use redundant interfaces (like `BookingService` -> `BookingServiceImpl`) for my service layer. Since there is only ever one implementation for these services, modern Spring Boot (using CGLIB class-based proxying) handles them perfectly without interfaces. This keeps the codebase lean and avoids the friction of updating two files for every signature change.
* **Manual Static Mappers**: I built simple, manual mapper utility classes with static methods (e.g., `CourseMapper`) to handle entity-to-DTO conversions. This isolates data translation from business logic, compiles instantly, and avoids adding compiler-level libraries like MapStruct or reflection-heavy runtimes like ModelMapper.
* **Zero DTO Boilerplate**: I used native Java 21 `record` classes for all request and response DTOs. They are immutable, thread-safe, and get rid of Lombok getter/setter clutter entirely.
* **Standard RFC Errors**: Instead of writing a custom wrapper class for error responses, I went with Spring Boot 3's native `ProblemDetail` (RFC 7807) inside the global exception handler. It formats all validation and business errors in a standard web format out of the box.

---

## ID Generation

I chose **service-layer UUID generation** (`UUID.randomUUID()` in mappers/services) as the primary approach.

Why this is better for this project:
* The app knows the booking ID immediately, so it can create `booking_session_lock` rows in the same flow without waiting on DB-generated IDs.
* It keeps the booking transaction simple and predictable under concurrency.
* It matches the service-first design we already use.

I also added a **database fallback** (`DEFAULT gen_random_uuid()`) so manual SQL inserts are safe too.
For race-safety, the database also enforces `UNIQUE(parent_id, session_id)` in `booking_session_lock` to prevent duplicate lock rows under concurrency.
