package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketPaymentServiceImpl mockTicketPaymentService;

    @Mock
    private SeatReservationServiceImpl mockSeatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Captor
    private ArgumentCaptor<Long> idArgumentCaptor;

    @Captor
    private ArgumentCaptor<Integer> paymentArgumentCaptor;

    @Captor
    private ArgumentCaptor<Integer> totalSeatsToAllocate;


    private final TicketTypeRequest validAdult2TicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
    private final TicketTypeRequest validChild2TicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
    private final TicketTypeRequest validInfant2TicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

    @BeforeEach
    void setup() {
        ticketService = new TicketServiceImpl(mockTicketPaymentService, mockSeatReservationService);
    }

    @Test
    public void test_should_throw_exception_when_accountId_is_null() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(null, validAdult2TicketRequest, validChild2TicketRequest);
        });
        assertEquals("Invalid account ID.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_accountId_is_zero() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(0L, validAdult2TicketRequest, validChild2TicketRequest);
        });
        assertEquals("Invalid account ID.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_request_is_null() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, null);
        });
        assertEquals("Ticket type request can't be null.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_one_of_ticket_request_is_null() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, validAdult2TicketRequest, null);
        });
        assertEquals("Ticket type request can't be null.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_adult_with_zero_ticket() {
        TicketTypeRequest invalidAdultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, invalidAdultTicketRequest, validChild2TicketRequest);
        });
        assertEquals("Invalid ticket number, must be positive integer.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_ticket_contains_negative_number() {
        TicketTypeRequest invalidChildTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, -1);
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, validAdult2TicketRequest, invalidChildTicketRequest);
        });
        assertEquals("Invalid ticket number, must be positive integer.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_no_ticket_is_included() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L);
        });
        assertEquals("Ticket booking should contain at least one adult.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_no_adult_is_included() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, validChild2TicketRequest);
        });
        assertEquals("Ticket booking should contain at least one adult.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_only_child_and_infant_are_included() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, validChild2TicketRequest, validInfant2TicketRequest);
        });
        assertEquals("Ticket booking should contain at least one adult.", exception.getMessage());
    }

    @Test
    public void test_should_throw_exception_when_total_number_of_tickets_more_than_25() {
        TicketTypeRequest validAdult23TicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 23);
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(123L, validAdult23TicketRequest, validChild2TicketRequest, validInfant2TicketRequest);
        });
        assertEquals("Maximum only 25 tickets allowed.", exception.getMessage());
    }

    @Test
    public void test_valid_request_call_third_party_services_with_right_data() {
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult2TicketRequest, validChild2TicketRequest, validInfant2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(80, paymentArgumentCaptor.getValue()); // 25*2 + 15*2 + 0*2 = 80
        assertEquals(4, totalSeatsToAllocate.getValue());
    }

    @Test
    public void test_valid_request_with_two_adult_ticket_request_call_third_party_services_with_right_data() {
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult2TicketRequest, validAdult2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(100, paymentArgumentCaptor.getValue()); // 25*2 + 25*2 = 100
        assertEquals(4, totalSeatsToAllocate.getValue());
    }

    @Test
    public void test_valid_request_with_only_adult_ticket_request_call_third_party_services_with_right_data() {
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(50, paymentArgumentCaptor.getValue()); // 25*2 = 50
        assertEquals(2, totalSeatsToAllocate.getValue());
    }

    @Test
    public void test_valid_request_with_adult_and_child_ticket_request_call_third_party_services_with_right_data() {
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult2TicketRequest, validChild2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(80, paymentArgumentCaptor.getValue()); // 25*2 + 15*2 = 80
        assertEquals(4, totalSeatsToAllocate.getValue());
    }

    @Test
    public void test_valid_request_with_adult_and_infant_ticket_request_call_third_party_services_with_right_data() {
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult2TicketRequest, validInfant2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(50, paymentArgumentCaptor.getValue()); // 25*2 = 50
        assertEquals(2, totalSeatsToAllocate.getValue());
    }

    @Test
    public void test_valid_request_with_25_including_infant_ticket_request_call_third_party_services_with_right_data() {
        TicketTypeRequest validAdult21TicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 21);
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult21TicketRequest, validChild2TicketRequest, validInfant2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(555, paymentArgumentCaptor.getValue()); // 25*21 + 15*2 + 0*2 = 555
        assertEquals(23, totalSeatsToAllocate.getValue());
    }

    @Test
    public void test_valid_request_with_25_excluding_infant_ticket_request_call_third_party_services_with_right_data() {
        TicketTypeRequest validAdult23TicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 23);
        doNothing().when(mockTicketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(mockSeatReservationService).reserveSeat(anyLong(), anyInt());
        ticketService.purchaseTickets(123L, validAdult23TicketRequest, validChild2TicketRequest);
        verify(mockTicketPaymentService).makePayment(idArgumentCaptor.capture(), paymentArgumentCaptor.capture());
        verify(mockSeatReservationService).reserveSeat(idArgumentCaptor.capture(), totalSeatsToAllocate.capture());
        assertEquals(123L, idArgumentCaptor.getValue());
        assertEquals(605, paymentArgumentCaptor.getValue()); // 23*25 + 2*15 = 605
        assertEquals(25, totalSeatsToAllocate.getValue());
    }
}
