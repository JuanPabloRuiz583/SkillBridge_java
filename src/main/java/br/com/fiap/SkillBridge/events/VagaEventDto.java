package br.com.fiap.SkillBridge.events;

import java.io.Serializable;
import java.time.Instant;

public class VagaEventDto implements Serializable {
    private Long id;
    private String action; // "CREATED", "UPDATED", "DELETED"
    private Instant timestamp;

    public VagaEventDto() {}

    public VagaEventDto(Long id, String action) {
        this.id = id;
        this.action = action;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public Instant getTimestamp() { return timestamp; }

    public void setId(Long id) { this.id = id; }
    public void setAction(String action) { this.action = action; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}