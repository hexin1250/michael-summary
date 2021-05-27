package michael.slf4j.investment.source;

import michael.slf4j.investment.model.Timeseries;

public interface ISource {
	public Timeseries retrieveData(String security);

}
