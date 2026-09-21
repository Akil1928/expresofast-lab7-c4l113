package cr.ac.ucr.paraiso.ie.c4l113.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.BitacoraEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BitacoraEnvioRepository extends JpaRepository<BitacoraEnvio, Integer> {
    List<BitacoraEnvio> findByEnvioIdOrderByFechaCambioDesc(Integer envioId);
}