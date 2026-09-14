package cr.ac.ucr.paraiso.ie.c4l113_c4h741.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4l113_c4h741.expresofast.domain.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Integer> {
}
