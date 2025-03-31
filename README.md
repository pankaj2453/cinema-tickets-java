# cinema-tickets-java
Cinema ticket coding exercise

* `TicketTypeRequest` is not touched assuming we wanted to keep this as it is.
* `varargs` `TicketTypeRequest` only iterated once to validate and get the required information to call the third party services.
* `TicketTypeRequest` record created to contain the information required to validate request and to call the third party services.
* `InvalidPurchaseException` is the only exception thrown but with different messages to understand the root cause.
* Test coverage is 100%.
* Ticket prices and maximum ticket limit could have been externalized as configurable values, but I assume it's not the in the scope of ths exercise.

Validations:
* This will throw `InvalidPurchaseException` in the following scenarios:
  - If accountId is null or negative
  - If varargs `TicketTypeRequest` is null or contains any null object.
  - If `TicketTypeRequest` contains any 0 or negative number of tickets.
  - If total number of tickets exceeds 25.
  - If request doesn't contain any adult type ticket.