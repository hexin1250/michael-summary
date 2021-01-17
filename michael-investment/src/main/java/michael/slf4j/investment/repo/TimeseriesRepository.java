package michael.slf4j.investment.repo;

import java.sql.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import michael.slf4j.investment.model.TimeseriesModel;

public interface TimeseriesRepository extends CrudRepository<TimeseriesModel, Integer> {
	List<TimeseriesModel> findBySecurity(String security);
	List<TimeseriesModel> findByTradeDate(Date tradeDate);
}
