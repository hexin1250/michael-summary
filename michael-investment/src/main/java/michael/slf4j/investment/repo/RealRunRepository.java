package michael.slf4j.investment.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import michael.slf4j.investment.model.RealRun;

public interface RealRunRepository extends CrudRepository<RealRun, Long> {
	RealRun findByName(String name);
	
	@Query(value = "select * from real_run where end_time is null", nativeQuery = true)
	List<RealRun> findRunningJobs();

}
