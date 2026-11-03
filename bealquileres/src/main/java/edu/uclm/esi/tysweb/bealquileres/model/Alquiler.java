package edu.uclm.esi.tysweb.bealquileres.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alquiler")
public class Alquiler {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false)
    private String emailUsuario;

    @Column(nullable = false, unique = true)
    private String paymentIntentId;

    @Column(nullable = false)
    private Integer horas;

    @Column(nullable = false)
    private Long importeCentimos;

    @Column(nullable = false)
    private LocalDateTime horaInicio;

    @Column(nullable = false)
    private LocalDateTime horaFin;

    protected Alquiler() {
    }

    public Alquiler(
            Vehiculo vehiculo,
            String emailUsuario,
            String paymentIntentId,
            Integer horas,
            Long importeCentimos,
            LocalDateTime horaInicio) {
        this.vehiculo = vehiculo;
        this.emailUsuario = emailUsuario;
        this.paymentIntentId = paymentIntentId;
        this.horas = horas;
        this.importeCentimos = importeCentimos;
        this.horaInicio = horaInicio;
        this.horaFin = horaInicio.plusHours(horas);
    }

    public Long getId() {
        return id;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public Integer getHoras() {
        return horas;
    }

    public Long getImporteCentimos() {
        return importeCentimos;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public LocalDateTime getHoraFin() {
        return horaFin;
    }
}
