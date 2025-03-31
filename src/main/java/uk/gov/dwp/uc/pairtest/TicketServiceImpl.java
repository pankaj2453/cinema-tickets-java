package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketInformation;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.constants.Constants.CHILD_TICKET_PRICE;
import static uk.gov.dwp.uc.pairtest.constants.Constants.ADULT_TICKET_PRICE;
import static uk.gov.dwp.uc.pairtest.constants.Constants.MAX_ALLOWED_TICKETS;
import static uk.gov.dwp.uc.pairtest.constants.Constants.ERROR_MAX_ALLOWED_TICKETS;
import static uk.gov.dwp.uc.pairtest.constants.Constants.ERROR_NO_ADULT;
import static uk.gov.dwp.uc.pairtest.constants.Constants.ERROR_INVALID_REQUEST;
import static uk.gov.dwp.uc.pairtest.constants.Constants.ERROR_INVALID_ID;
import static uk.gov.dwp.uc.pairtest.constants.Constants.ERROR_INVALID_TICKET_NUMBER;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private final SeatReservationService seatReservationService;
    private final TicketPaymentService ticketPaymentService;


    public TicketServiceImpl(
            TicketPaymentService ticketPaymentService,
            SeatReservationService seatReservationService
    ) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(ERROR_INVALID_ID);
        }

        TicketInformation ticketInformation = getTicketInformation(ticketTypeRequests);
        seatReservationService.reserveSeat(accountId, ticketInformation.totalNumberOfSeats());
        ticketPaymentService.makePayment(accountId, ticketInformation.totalPrice());
    }

    private TicketInformation getTicketInformation(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        int totalPrice = 0;
        int totalNumberOfSeats = 0;
        int totalNumberOfTickets = 0;
        boolean isAdultIncluded = false;
        if (ticketTypeRequests == null) {
            throw new InvalidPurchaseException(ERROR_INVALID_REQUEST);
        }
        for (TicketTypeRequest ticketRequest : ticketTypeRequests) {
            if (ticketRequest == null) {
                throw new InvalidPurchaseException(ERROR_INVALID_REQUEST);
            }

            int numberOfTickets = ticketRequest.getNoOfTickets();

            if (numberOfTickets <= 0) {
                throw new InvalidPurchaseException(ERROR_INVALID_TICKET_NUMBER);
            }

            totalNumberOfTickets += numberOfTickets;
            switch (ticketRequest.getTicketType()) {
                case CHILD:
                    totalPrice += CHILD_TICKET_PRICE * numberOfTickets;
                    totalNumberOfSeats += numberOfTickets;
                    break;
                case ADULT:
                    isAdultIncluded = true;
                    totalPrice += ADULT_TICKET_PRICE * numberOfTickets;
                    totalNumberOfSeats += numberOfTickets;
                    break;
                case INFANT:
                    // Infants only count towards total tickets
                    break;
            }
        }

        if (!isAdultIncluded) {
            throw new InvalidPurchaseException(ERROR_NO_ADULT);
        }
        if (totalNumberOfTickets > MAX_ALLOWED_TICKETS) {
            throw new InvalidPurchaseException(ERROR_MAX_ALLOWED_TICKETS);
        }
        return new TicketInformation(totalPrice, totalNumberOfSeats);
    }

}
