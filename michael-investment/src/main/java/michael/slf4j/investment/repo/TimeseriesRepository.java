package michael.slf4j.investment.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import michael.slf4j.investment.model.Timeseries;

public interface TimeseriesRepository extends CrudRepository<Timeseries, Integer> {
	List<Timeseries> findBySecurity(String security);
	List<Timeseries> findByTradeDate(String tradeDate);
	
	@Query(value = "select distinct security from timeseries where variety = ?1 and trade_date = ?2 and is_main_future = 'T'", nativeQuery = true)
	String findMainFutureByVarietyDate(@Param("variety") String variety, @Param("tradeDate") String tradeDate);
	
	@Query(value = "select * from timeseries where security = ?1 and trade_date >= ?2 and trade_date <= ?3 and freq = ?4 order by trade_ts", nativeQuery = true)
	List<Timeseries> findByTradeDateWithPeriod(@Param("security") String security, @Param("start") String start, @Param("end") String end, @Param("freq") String freq);
	
	@Query(value = "select * from timeseries where security = :security and trade_date < :tradeDate and freq = :freq order by trade_ts desc limit :limit", nativeQuery = true)
	List<Timeseries> findByTradeDateWithPeriodLimit(@Param("security") String security, @Param("tradeDate") String tradeDate, @Param("freq") String freq, @Param("limit") int limit);
	
	@Query(value = "select * from timeseries where security = ?1 and trade_date = ?2 and freq = ?3 order by trade_ts", nativeQuery = true)
	List<Timeseries> findByTradeDateWithPeriod(@Param("security") String security, @Param("tradeDate") String tradeDate, @Param("freq") String freq);
	
	@Query(value = "select distinct trade_date from timeseries order by trade_date", nativeQuery = true)
	List<String> findAllTradeDate();
	
	@Query(value = "select distinct trade_date from timeseries where variety = ?1 order by trade_date", nativeQuery = true)
	List<String> findAllTradeDateByVariety(@Param("variety") String variety);
	
	@Query(value = "select distinct trade_date from timeseries where variety = ?1 order by trade_date desc limit 2", nativeQuery = true)
	List<String> findMaxTradeDate(@Param("variety") String variety);
	
	@Query(value = "select distinct security from timeseries where variety = ?1 and trade_date = ?2", nativeQuery = true)
	List<String> findSecurities(@Param("variety") String variety, @Param("tradeDate") String tradeDate);
	
	@Query(value = "select * from timeseries where security in (:securities) and trade_date = :tradeDate and freq = :freq", nativeQuery = true)
	List<Timeseries> findSecuritiesBySecurities(@Param("securities") List<String> securities, @Param("tradeDate") String tradeDate, @Param("freq") String freq);
	
	@Query(value = "select * from timeseries where variety = ?1 and trade_date = ?2 and freq = ?3", nativeQuery = true)
	List<Timeseries> findSecuritiesByVTF(@Param("variety") String variety, @Param("tradeDate") String tradeDate, @Param("freq") String freq);
	
	@Query(value = "select security from timeseries where open_interest = (select max(b.open_interest) from timeseries as b where variety = ?1 and trade_date = ?2 and freq = '1D') and freq = '1D'", nativeQuery = true)
	String findPrimarySecurity(@Param("variety") String variety, @Param("tradeDate") String tradeDate);
	
	@Query(value = "select * from timeseries where security = :security and trade_date = :tradeDate and freq = '1D'", nativeQuery = true)
	Timeseries findDailySecurityByTradeDate(@Param("security") String security, @Param("tradeDate") String tradeDate);

}
