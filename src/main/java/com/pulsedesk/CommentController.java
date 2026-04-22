package com.pulsedesk;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final HuggingFaceService huggingFaceService;

    public CommentController(CommentRepository commentRepository,
                             TicketRepository ticketRepository,
                             HuggingFaceService huggingFaceService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.huggingFaceService = huggingFaceService;
    }

    @PostMapping
    public Comment createComment(@RequestBody Comment comment) throws Exception {
        Comment saved = commentRepository.save(comment);

        TicketAnalysis analysis = huggingFaceService.analyzeComment(comment.getContent());

        if (analysis.getIsTicket()) {
            Ticket ticket = new Ticket();
            ticket.setTitle(analysis.getTitle());
            ticket.setCategory(analysis.getCategory());
            ticket.setPriority(analysis.getPriority());
            ticket.setSummary(analysis.getSummary());
            ticketRepository.save(ticket);
        }

        return saved;
    }

    @GetMapping
    public List<Comment> getComments() {
        return commentRepository.findAll();
    }
}