package michael.slf4j.investment.quant.formula;

import java.util.Collection;
import java.util.Map;

import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;

public interface IFormula<T> {
	public Map<String, Map<String, T>> getModel(Security security, Collection<Timeseries> securityList);
	public Map<String, Map<String, T>> getModel(String security, Collection<Timeseries> securityList);

}
