package michael.slf4j.investment.parse;

import java.util.List;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;

public interface IParser {
	public List<Timeseries> parse(String content, FreqEnum freq);
	public List<Timeseries> parse(Security security, String content, FreqEnum freq);

}
