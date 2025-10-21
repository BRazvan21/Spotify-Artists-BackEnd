package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@Entity
@Table(name = "baza_de_date")
public class BazaDeDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nume;

    private String ArtistID;

    private int versiune;

    public BazaDeDate() {}

    public BazaDeDate(String nume, String ArtistID, int versiune) {
        this.nume = nume;
        this.ArtistID = ArtistID;
        this.versiune = versiune;
    }

    // Getters și setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNume() {
        return nume;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    public String getArtistID() {
        return ArtistID;
    }

    public void getArtistID(String ArtistID) {
        this.ArtistID = ArtistID;
    }

    public int getVersiune() {
        return versiune;
    }

    public void setVersiune(int versiune) {
        this.versiune = versiune;
    }
}
