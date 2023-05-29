package michael.slf4j.investment.parse;

import java.util.List;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;

public interface IParser {
	public List<Timeseries> parse(String content, FreqEnum freq);

}
