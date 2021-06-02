package michael.slf4j.investment.config;

import java.util.Comparator;

public class TradingTimeComparator implements Comparator<TradingTimeConfig> {

	@Override
	public int compare(TradingTimeConfig o1, TradingTimeConfig o2) {
		int com = o1.getPriority() - o2.getPriority();
		if(com == 0) {
			if(o1.isFull() || o2.isFull()) {
				return 1;
			}
			com = o1.getStart().compareTo(o2.getStart());
		}
		return com;
	}

}
