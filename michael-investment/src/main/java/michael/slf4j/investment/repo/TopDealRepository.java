package michael.slf4j.investment.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import michael.slf4j.investment.model.TopDeal;

public interface TopDealRepository extends CrudRepository<TopDeal, Long> {
	@Query(value = "select * from top_deal where security = :security and trade_date in (:tradeDates) order by trade_date desc", nativeQuery = true)
	List<TopDeal> findSecuritiesBySecurities(@Param("security") String security, @Param("tradeDates") List<String> tradeDates);

}
