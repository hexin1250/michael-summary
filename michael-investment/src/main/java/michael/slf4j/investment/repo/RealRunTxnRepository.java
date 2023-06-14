package michael.slf4j.investment.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import michael.slf4j.investment.model.RealRunTxn;

public interface RealRunTxnRepository extends CrudRepository<RealRunTxn, Long> {
	@Query(value = "select * from real_run_txn where real_run_id = :realRunId order by trade_ts", nativeQuery = true)
	List<RealRunTxn> findByRealRunId(@Param("realRunId") Long realRunId);

}
