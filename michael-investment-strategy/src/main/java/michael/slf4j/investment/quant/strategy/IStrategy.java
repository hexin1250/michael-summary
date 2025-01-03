package michael.slf4j.investment.quant.strategy;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Variety;

public interface IStrategy extends Serializable {
	public void init();
	public void initContext(Context context);
	public Context getContext();
	public int getHistoricalSize();
	
	public void before();
	public void handle(Account acc, Bar bar);
	public void after();
	public List<Security> subscriberList(LocalDate tradeDate);
	public void setMessageService(MessageService messageService);
	public void freq15M(Security security, List<Contract> list);
	public void freq30M(Account acc, Bar bar, Variety variety, Security security, List<Contract> list);
	public void freq60M(Security security, List<Contract> list);
	
	public default void subscriber(Context context, LocalDate tradeDate) {
		List<Security> list = subscriberList(tradeDate);
		context.historical.subscribe(list, tradeDate);
	}

}
