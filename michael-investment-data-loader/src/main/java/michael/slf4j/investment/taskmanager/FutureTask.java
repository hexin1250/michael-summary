package michael.slf4j.investment.taskmanager;

import java.io.IOException;

import org.apache.log4j.Logger;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.source.SinaSource;
import michael.slf4j.investment.util.TradeUtil;

public class FutureTask implements Runnable {
	private static final Logger log = Logger.getLogger(FutureTask.class);

	private FutureLoader futureLoader;
	private SinaSource sinaSource;
	private String variety;
	private String security;
	
	public FutureTask(FutureLoader futureLoader, SinaSource sinaSource, String variety, String security) {
		this.futureLoader = futureLoader;
		this.sinaSource = sinaSource;
		this.security = security;
		this.variety = variety;
	}

	@Override
	public void run() {
		try {
			String content = sinaSource.getContent(security);
			if(content.length() > 0 && TradeUtil.isTradingTime()) {
				futureLoader.load(variety, security, content, FreqEnum._TICK);
			}
		} catch (IOException e) {
			log.error("错误发生！", e);
		}
	}

}
