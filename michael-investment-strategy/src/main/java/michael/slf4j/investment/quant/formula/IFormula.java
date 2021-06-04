package michael.slf4j.investment.quant.formula;

import java.time.LocalDate;

import michael.slf4j.investment.model.Security;

public interface IFormula<T> {
	public T getModel(Security security, LocalDate tradeDate);

}
