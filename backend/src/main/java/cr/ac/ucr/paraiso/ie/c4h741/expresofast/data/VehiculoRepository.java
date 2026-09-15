package cr.ac.ucr.paraiso.ie.c4h741.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Integer> {
}
