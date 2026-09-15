package cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehiculo_id")
    private Integer id;

    @Column(name = "placa", nullable = false, unique = true, length = 15)
    private String placa;

    @Column(name = "capacidad_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadKg;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado; // DISPONIBLE, EN_RUTA, MANTENIMIENTO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaLogistica empresa;

    @JsonIgnore
    @OneToMany(mappedBy = "vehiculo")
    private List<Envio> envios = new ArrayList<>();

    // ---- Constructores ----
    public Vehiculo() {
    }

    // ---- Getters y Setters ----
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public BigDecimal getCapacidadKg() {
        return capacidadKg;
    }

    public void setCapacidadKg(BigDecimal capacidadKg) {
        this.capacidadKg = capacidadKg;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public EmpresaLogistica getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaLogistica empresa) {
        this.empresa = empresa;
    }

    public List<Envio> getEnvios() {
        return envios;
    }

    public void setEnvios(List<Envio> envios) {
        this.envios = envios;
    }
}