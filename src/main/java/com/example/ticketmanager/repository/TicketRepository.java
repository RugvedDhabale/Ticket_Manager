package com.example.ticketmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ticketmanager.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
    