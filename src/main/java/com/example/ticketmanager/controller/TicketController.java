package com.example.ticketmanager.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ticketmanager.entity.Ticket;
import com.example.ticketmanager.repository.TicketRepository;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin
public class TicketController {

    private final TicketRepository ticketRepository;
    private static final String UPLOAD_DIR = "uploads";

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /* ---------------- CREATE (with attachment) ---------------- */
    @PostMapping(consumes = "multipart/form-data")
    public Ticket createTicket(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String priority,
            @RequestParam(required = false) MultipartFile file
    ) {

        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");

        try {
            if (file != null && !file.isEmpty()) {
                Files.createDirectories(Paths.get(UPLOAD_DIR));

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR, fileName);

                file.transferTo(filePath.toFile());
                ticket.setAttachmentPath(filePath.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ticketRepository.save(ticket);
    }

    /* ---------------- CREATE (JSON for bulk upload) ---------------- */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Ticket> createTicketJson(@RequestBody Ticket ticket) {
        System.out.println("Creating ticket via JSON: " + ticket.getTitle());
        
        // Set default status if not provided
        if (ticket.getStatus() == null || ticket.getStatus().isEmpty()) {
            ticket.setStatus("OPEN");
        }
        
        // Validate priority
        String priority = ticket.getPriority();
        if (priority == null || (!priority.equals("LOW") && !priority.equals("MEDIUM") && !priority.equals("HIGH"))) {
            ticket.setPriority("LOW");
        }
        
        Ticket saved = ticketRepository.save(ticket);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(
        value = "/bulk-upload",
        consumes = "multipart/form-data"
    )
    public ResponseEntity<?> bulkUploadTickets(
            @RequestParam("file") MultipartFile file
    )  
    {

        System.out.println("=== BULK UPLOAD REQUEST RECEIVED ===");

        if (file.isEmpty()) {
            System.err.println("Error: File is empty");
            return ResponseEntity.badRequest().body("File is empty");
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            System.err.println("Error: Not a CSV file");
            return ResponseEntity.badRequest().body("File must be a CSV");
        }

        List<Ticket> createdTickets = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            String[] headers = null;
            int titleIndex = -1;
            int descriptionIndex = -1;
            int priorityIndex = -1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.isEmpty()) {
                    continue;
                }

                // Parse header
                if (headers == null) {
                    headers = parseCsvLine(line);
                    System.out.println("CSV Headers: " + String.join(", ", headers));
                    
                    // Find column indices (case-insensitive)
                    for (int i = 0; i < headers.length; i++) {
                        String header = headers[i].toLowerCase().trim();
                        if (header.equals("title")) titleIndex = i;
                        if (header.equals("description")) descriptionIndex = i;
                        if (header.equals("priority")) priorityIndex = i;
                    }

                    // Validate required columns
                    if (titleIndex == -1 || descriptionIndex == -1 || priorityIndex == -1) {
                        return ResponseEntity.badRequest().body(
                            "CSV must have columns: Title, Description, and Priority"
                        );
                    }
                    continue;
                }

                // Parse data rows
                try {
                    String[] values = parseCsvLine(line);
                    
                    if (values.length < 3) {
                        errors.add("Line " + lineNumber + ": Insufficient columns");
                        continue;
                    }

                    Ticket ticket = new Ticket();
                    ticket.setTitle(values[titleIndex].trim());
                    ticket.setDescription(values[descriptionIndex].trim());
                    
                    String priority = values[priorityIndex].trim().toUpperCase();
                    if (!priority.equals("LOW") && !priority.equals("MEDIUM") && !priority.equals("HIGH")) {
                        priority = "LOW";
                        System.out.println("Line " + lineNumber + ": Invalid priority, defaulting to LOW");
                    }
                    ticket.setPriority(priority);
                    ticket.setStatus("OPEN");

                    // Validate required fields
                    if (ticket.getTitle().isEmpty() || ticket.getDescription().isEmpty()) {
                        errors.add("Line " + lineNumber + ": Title and Description cannot be empty");
                        continue;
                    }

                    Ticket saved = ticketRepository.save(ticket);
                    createdTickets.add(saved);
                    System.out.println("✓ Created ticket: " + saved.getTitle());

                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    System.err.println("Error on line " + lineNumber + ": " + e.getMessage());
                }
            }

            System.out.println("=== BULK UPLOAD COMPLETED ===");
            System.out.println("Total created: " + createdTickets.size());
            System.out.println("Total errors: " + errors.size());

            // Build response
            BulkUploadResponse response = new BulkUploadResponse();
            response.setSuccessCount(createdTickets.size());
            response.setFailureCount(errors.size());
            response.setErrors(errors);
            response.setCreatedTickets(createdTickets);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Fatal error during bulk upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing CSV: " + e.getMessage());
        }
    }

    /* ---------------- CSV PARSING HELPER ---------------- */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    /* ---------------- BULK UPLOAD RESPONSE CLASS ---------------- */
    public static class BulkUploadResponse {
        private int successCount;
        private int failureCount;
        private List<String> errors;
        private List<Ticket> createdTickets;

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<Ticket> getCreatedTickets() { return createdTickets; }
        public void setCreatedTickets(List<Ticket> createdTickets) { this.createdTickets = createdTickets; }
    }

    /* ---------------- READ ALL ---------------- */
    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    /* ---------------- READ BY ID ---------------- */
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable Long id,
            @RequestBody Ticket updated) {

        return ticketRepository.findById(id).map(ticket -> {
            ticket.setTitle(updated.getTitle());
            ticket.setDescription(updated.getDescription());
            ticket.setPriority(updated.getPriority());
            ticket.setStatus(updated.getStatus());
            return ResponseEntity.ok(ticketRepository.save(ticket));
        }).orElse(ResponseEntity.notFound().build());
    }

    /* ---------------- DELETE (SAFE) ---------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTicket(@PathVariable Long id) {
        System.out.println("=== DELETE REQUEST RECEIVED ===");
        System.out.println("Ticket ID to delete: " + id);
        System.out.println("ID type: " + id.getClass().getName());
        
        return ticketRepository.findById(id).map(ticket -> {
            System.out.println("Ticket found in database: " + ticket.getTitle());
            
            try {
                // Delete attachment file if it exists
                if (ticket.getAttachmentPath() != null && !ticket.getAttachmentPath().isEmpty()) {
                    Path path = Paths.get(ticket.getAttachmentPath());
                    System.out.println("Attachment path: " + path);
                    if (Files.exists(path)) {
                        Files.delete(path);
                        System.out.println("✓ Deleted file: " + path);
                    } else {
                        System.out.println("⚠ File not found, skipping: " + path);
                    }
                } else {
                    System.out.println("No attachment to delete");
                }
            } catch (Exception e) {
                System.err.println("✗ Failed to delete attachment file: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Delete from database
            try {
                ticketRepository.delete(ticket);
                System.out.println("✓ Deleted ticket ID: " + id + " from database");
            } catch (Exception e) {
                System.err.println("✗ Failed to delete from database: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(500).body("Database deletion failed");
            }
            
            System.out.println("=== DELETE COMPLETED SUCCESSFULLY ===");
            return ResponseEntity.noContent().build();
            
        }).orElseGet(() -> {
            System.err.println("✗ Ticket not found with ID: " + id);
            System.out.println("=== DELETE FAILED - NOT FOUND ===");
            return ResponseEntity.notFound().build();
        });
    }
}