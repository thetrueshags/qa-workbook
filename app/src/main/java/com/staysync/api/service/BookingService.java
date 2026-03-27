package com.staysync.api.service;

import com.staysync.api.dto.BookingRequest;
import com.staysync.api.model.Booking;
import com.staysync.api.model.Booking.BookingStatus;
import com.staysync.api.model.Guest;
import com.staysync.api.model.Room;
import com.staysync.api.repository.BookingRepository;
import com.staysync.api.repository.GuestRepository;
import com.staysync.api.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          GuestRepository guestRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    public List<Booking> getBookingsByGuestId(Long guestId) {
        return bookingRepository.findByGuestId(guestId);
    }

    public List<Booking> getBookingsByRoomId(Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        Guest guest = guestRepository.findById(request.getGuestId())
                .orElseThrow(() -> new RuntimeException("Guest not found with id: " + request.getGuestId()));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + request.getRoomId()));

        if (!room.isAvailable()) {
            throw new RuntimeException("Room " + room.getRoomNumber() + " is not available");
        }

        BigDecimal totalPrice = calculateTotalPrice(
                request.getCheckInDate(),
                request.getCheckOutDate(),
                room.getPricePerNight()
        );

        Booking booking = new Booking();
        booking.setGuest(guest);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        room.setAvailable(false);
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long id) {
        Booking booking = getBookingById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(LocalDate checkIn, LocalDate checkOut, BigDecimal pricePerNight) {
        long numberOfNights = ChronoUnit.DAYS.between(checkIn, checkOut) - 1;
        return pricePerNight.multiply(BigDecimal.valueOf(numberOfNights));
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByIsAvailableTrue();
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}
