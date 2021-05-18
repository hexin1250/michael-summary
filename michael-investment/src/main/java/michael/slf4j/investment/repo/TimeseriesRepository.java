package michael.slf4j.investment.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import michael.slf4j.investment.model.TimeseriesModel;

public interface TimeseriesRepository extends CrudRepository<TimeseriesModel, Integer> {
	List<TimeseriesModel> findBySecurity(String security);
	List<TimeseriesModel> findByTradeDate(String tradeDate);
	
	@Query(value = "select * from timeseries where variety = ?1 and trade_date = ?2 and is_main_future = 'T' and freq = '1D'", nativeQuery = true)
	TimeseriesModel findMainFutureByVarietyDate(@Param("variety") String variety, @Param("tradeDate") String tradeDate);
	
	@Query(value = "select * from timeseries where security = ?1 and trade_date >= ?2 and trade_date <= ?3 and freq = ?4 order by trade_ts", nativeQuery = true)
	List<TimeseriesModel> findByTradeDateWithPeriod(@Param("security") String security, @Param("start") String start, @Param("end") String end, @Param("freq") String freq);
	
	@Query(value = "select * from timeseries where security = ?1 and trade_date = ?2 and freq = ?3 order by trade_ts", nativeQuery = true)
	List<TimeseriesModel> findByTradeDateWithPeriod(@Param("security") String security, @Param("tradeDate") String tradeDate, @Param("freq") String freq);
	
	@Query(value = "select distinct trade_date from timeseries order by trade_date", nativeQuery = true)
	List<String> findAllTradeDate();
	
	@Query(value = "select distinct trade_date from timeseries where variety = ?1 order by trade_date", nativeQuery = true)
	List<String> findAllTradeDateByVariety(@Param("variety") String variety);
	
	@Query(value = "select distinct trade_date from timeseries where variety = ?1 order by trade_date desc limit 2", nativeQuery = true)
	List<String> findMaxTradeDate(@Param("variety") String variety);
	
	@Query(value = "select distinct security from timeseries where variety = ?1 and trade_date = ?2", nativeQuery = true)
	List<String> findSecurities(@Param("variety") String variety, @Param("tradeDate") String tradeDate);

}
