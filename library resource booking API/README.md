Library Booking API

Project Overview

Library Booking API is a Spring Boot REST application that manages resource borrowing, waitlists, reservations, and reservation claims for a library system.

The system allows employees to borrow available resources, be automatically queued when resources are unavailable, receive reservations automatically when resources are returned, and claim reservations before they expire. When a reservation expires unclaimed, the system automatically advances the waitlist queue to the next person.

⸻

Features

Employee Management

* Create employees
* View all employees
* View an employee's active borrows
* View an employee's pending reservations

Resource Management

* Create resources (books, equipment, AV kits)
* View all resources

Borrow Management

* Borrow available resources
* Return borrowed resources with confirmation message
* Prevent duplicate active borrows
* Auto-add to waitlist when no copies are available

Waitlist Management

* Automatically join waitlist when borrowing an unavailable resource
* Manually join or leave a waitlist
* FIFO (First-In First-Out) queue processing
* Prevent duplicate waitlist entries

Reservation Management

* Automatically create reservations when resources are returned
* Reservation expiration after 2 hours
* Auto-cascade queue when a reservation expires
* Claim valid reservations
* Prevent claiming expired or another employee's reservation

⸻

Technologies Used

* Java 17
* Spring Boot 3
* Spring Data JPA
* H2 File Database
* Lombok
* Maven

⸻

How to Run Locally

Prerequisites

* Java 17 or higher installed
* Maven (or use the included wrapper)

Steps

1. Clone the repository:

git clone https://github.com/SAIFMOHSIN8/library-booking-api.git
cd library-booking-api

2. Build the project:

On Windows:
mvnw.cmd clean package

On Mac/Linux:
./mvnw clean package

3. Run the application:

On Windows:
mvnw.cmd spring-boot:run

On Mac/Linux:
./mvnw spring-boot:run

4. The application starts at:

http://localhost:8080

5. Access the H2 database console at:

http://localhost:8080/h2-console

JDBC URL:  jdbc:h2:file:./data/librarydb
Username:  sa
Password:  (leave blank)

⸻

API Endpoints and Postman Guide

Below is every endpoint with its method, URL, and how to call it from Postman.

------------------------------------------------------------
EMPLOYEE ENDPOINTS
------------------------------------------------------------

1. Create Employee
   Method : POST
   URL    : http://localhost:8080/employees
   Body   : (raw JSON)
   {
     "name": "Alice",
     "email": "alice@example.com"
   }

2. Get All Employees
   Method : GET
   URL    : http://localhost:8080/employees
   Body   : none

3. View Employee's Active Borrows
   Method : GET
   URL    : http://localhost:8080/employees/1/borrows
   Body   : none
   Note   : Replace 1 with the actual employee ID.
            Returns all resources the employee currently has borrowed.

4. View Employee's Pending Reservations
   Method : GET
   URL    : http://localhost:8080/employees/1/reservations
   Body   : none
   Note   : Replace 1 with the actual employee ID.
            Returns all reservations the employee can still claim.

------------------------------------------------------------
RESOURCE ENDPOINTS
------------------------------------------------------------

5. Create Resource
   Method : POST
   URL    : http://localhost:8080/resources
   Body   : (raw JSON)
   {
     "title": "Clean Code",
     "type": "BOOK",
     "totalCopies": 2
   }
   Note   : type accepts BOOK, EQUIPMENT, or AV_KIT.

6. Get All Resources
   Method : GET
   URL    : http://localhost:8080/resources
   Body   : none

------------------------------------------------------------
BORROW ENDPOINTS
------------------------------------------------------------

7. Borrow a Resource
   Method : POST
   URL    : http://localhost:8080/borrow?employeeId=1&resourceId=1
   Body   : none
   Note   : Two possible responses:
            - 200 OK + BorrowRecord JSON  (copy was available, borrowed)
            - 202 Accepted + message      (no copies available, auto-added
              "Resource is not available and you have been added to the
               waiting list at position X")

8. Return a Resource
   Method : POST
   URL    : http://localhost:8080/borrow/return?borrowRecordId=1
   Body   : none
   Note   : Replace 1 with the borrow record ID.
            Response: "Thank you for returning the resource"
            If anyone is in the waitlist, a reservation is automatically
            created for the first person in the queue.

------------------------------------------------------------
WAITLIST ENDPOINTS
------------------------------------------------------------

9. Join Waitlist Manually
   Method : POST
   URL    : http://localhost:8080/waitlist?employeeId=1&resourceId=1
   Body   : none
   Note   : Use this if the employee was not auto-added via borrow.

10. Leave Waitlist
    Method : DELETE
    URL    : http://localhost:8080/waitlist?employeeId=1&resourceId=1
    Body   : none
    Note   : Deactivates the employee's active waitlist entry.
             The queue automatically adjusts — no position reordering needed.

------------------------------------------------------------
RESERVATION ENDPOINTS
------------------------------------------------------------

11. Claim a Reservation
    Method : POST
    URL    : http://localhost:8080/reservations/claim?reservationId=1&employeeId=1
    Body   : none
    Note   : Employees may only claim their own reservation.
             Claiming creates a new active borrow record.
             If the reservation is already expired, the next person in
             the waitlist automatically receives a new reservation.

12. Process Expired Reservations (Admin Utility)
    Method : POST
    URL    : http://localhost:8080/reservations/process-expired
    Body   : none
    Note   : Scans all resources for expired PENDING reservations and
             advances the queue for each one.
             This runs automatically on borrow and claim — use this
             endpoint only for manual cleanup when no other activity
             has occurred.

⸻

Reservation Expiry and Queue Advancement — How It Works

This is the core design challenge of the system.

The Problem

A simple "reserved_by" field on a resource cannot handle a queue. When
a reservation expires, three things must happen in the same step:

  1. Release the expired employee's hold on the copy.
  2. Find the next person in the waitlist (FIFO order).
  3. Create a brand new PENDING reservation for them with a fresh
     2-hour window.

If person two also lets their reservation expire, the same logic must
fire again for person three — and so on until someone claims it or
the waitlist is empty.

Our Solution — Trigger-Based Cascade (No Scheduler)

Rather than a background timer, the cascade fires automatically at two
points in normal API usage:

  Trigger 1 — On every borrow attempt (POST /borrow):
  Before checking how many copies are available, the system calls
  processExpiredReservations(resourceId). This releases any expired
  holds for that resource first, so they do not falsely count as
  occupied copies and block a legitimate borrow.

  Trigger 2 — On every claim attempt (POST /reservations/claim):
  If the employee calls claim and their reservation has already
  expired, the system immediately calls advanceQueue(resourceId).
  This creates a new PENDING reservation for the next person in the
  waitlist in the same HTTP request — no separate call needed.

Data Model

  Resource         — holds totalCopies
  BorrowRecord     — active=true while the copy is physically out
  WaitlistEntry    — active=true while the employee is waiting;
                     position number determines FIFO order
  Reservation      — PENDING while waiting to be claimed;
                     CLAIMED once the employee borrows;
                     EXPIRED if the 2-hour window passed

Availability Formula (in BorrowService)

  available = totalCopies - activeBorrows - pendingReservations

Pending reservations count as occupied to prevent two employees
from borrowing the same copy simultaneously.

Return Flow (what happens when a resource is returned)

  1. BorrowRecord.active set to false, returnedAt stamped.
  2. Waitlist queried for the lowest active position entry.
  3. If someone is waiting:
       - New PENDING Reservation created (expiresAt = now + 2 hours)
       - WaitlistEntry.active set to false (removed from queue)
  4. If nobody is waiting: copy becomes available for direct borrow.

Example Cascade

  Resource X has 1 copy.

  Step 1: Employee A borrows it.
  Step 2: Employee B tries to borrow → auto-added to waitlist (pos 1).
  Step 3: Employee C tries to borrow → auto-added to waitlist (pos 2).
  Step 4: Employee A returns → B gets a PENDING reservation.
  Step 5: B's reservation expires without being claimed.
  Step 6: Employee D tries to borrow → system detects B's expired
          reservation, releases it, gives C a new PENDING reservation,
          then D borrows the copy (now available).
  Step 7: C claims their reservation → new borrow record created for C.

⸻

Business Rules Summary

Borrowing Rules

* Employees can only borrow available resources.
* Employees cannot borrow the same resource more than once while
  an active borrow exists.
* Pending reservations reduce the available copy count.
* When no copies are available, the employee is automatically
  added to the waitlist.

Waitlist Rules

* Employees are auto-added to the waitlist on a failed borrow.
* Waitlist positions are assigned in FIFO order.
* Employees cannot join the same waitlist twice.
* Employees can leave the waitlist at any time.

Reservation Rules

* Returning a resource auto-creates a reservation for the first
  person in the waitlist.
* Reservations expire 2 hours after creation.
* Expired reservations auto-cascade to the next person in queue.
* Only the assigned employee may claim a reservation.
* Claiming a reservation creates a new borrow record.

⸻

Business Rule Validation Examples

Auto-Waitlist on Unavailable Resource

Request:
POST /borrow?employeeId=2&resourceId=1   (resource fully borrowed)

Expected Response (202 Accepted):
"Resource is not available and you have been added to the waiting list at position 1"

Duplicate Borrow Prevention

Request:
POST /borrow?employeeId=4&resourceId=3   (employee already has it)

Expected Response (400):
Employee already has this resource borrowed

Duplicate Waitlist Prevention

Request:
POST /waitlist?employeeId=4&resourceId=2  (already in queue)

Expected Response (400):
Employee is already in the waitlist for this resource

Claim Another Employee's Reservation

Request:
POST /reservations/claim?reservationId=2&employeeId=4

Expected Response (400):
You cannot claim another employee's reservation

Claim Expired Reservation

Request:
POST /reservations/claim?reservationId=3&employeeId=1

Expected Response (400):
Reservation expired
(The next person in the waitlist automatically receives a new reservation.)

⸻

Sample Workflow (end-to-end Postman test)

1.  POST /employees              → create Alice (id=1)
2.  POST /employees              → create Bob (id=2)
3.  POST /employees              → create Carol (id=3)
4.  POST /resources              → create book, totalCopies=1 (id=1)
5.  POST /borrow?employeeId=1&resourceId=1  → 200, Alice borrows
6.  POST /borrow?employeeId=2&resourceId=1  → 202, Bob auto-waitlisted (pos 1)
7.  POST /borrow?employeeId=3&resourceId=1  → 202, Carol auto-waitlisted (pos 2)
8.  GET  /employees/1/borrows               → shows Alice's active borrow
9.  POST /borrow/return?borrowRecordId=1    → "Thank you for returning the resource"
                                               Bob gets a reservation automatically
10. GET  /employees/2/reservations          → shows Bob's PENDING reservation
11. (In H2 console, set Bob's expiresAt to a past datetime to simulate expiry)
12. POST /reservations/claim?reservationId=1&employeeId=2
                                            → "Reservation expired"
                                               Carol auto-gets a new reservation
13. GET  /employees/3/reservations          → shows Carol's PENDING reservation
14. POST /reservations/claim?reservationId=2&employeeId=3
                                            → 200, Carol claims and borrows

⸻

Exception Handling

All business rule violations return HTTP 400 with a plain-text message.

* Employee not found
* Resource not found
* Employee already has this resource borrowed
* Employee is already in the waitlist for this resource
* Employee is not in the waitlist for this resource
* You cannot claim another employee's reservation
* Reservation expired

⸻

Author

Saif Al Mahrouqi
