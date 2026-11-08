package com.aar.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vet_clinics")
public class VetClinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected VetClinic() {
    }

    public VetClinic(String name, String address, String phone) {
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public void update(String name, String address, String phone, boolean active) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
}
