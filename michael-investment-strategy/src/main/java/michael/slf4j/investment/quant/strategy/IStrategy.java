package michael.slf4j.investment.quant.strategy;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Security;

public interface IStrategy extends Serializable {
	public Map<String, Object> getParams();
	public void before(Context context, LocalDate current);
	public void handle(Account acc, Bar bar);
	public void after(Context context, LocalDate current);
	public List<Security> subscriberList(LocalDate tradeDate);
	public int getHistoricalSize();
	
	public default void subscriber(Context context, LocalDate tradeDate) {
		List<Security> list = subscriberList(tradeDate);
		context.historical.subscribe(list, tradeDate);
	}

}
