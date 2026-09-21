package cr.ac.ucr.paraiso.ie.c4l113.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Integer> {
}
