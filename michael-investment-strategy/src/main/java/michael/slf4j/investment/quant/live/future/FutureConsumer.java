package michael.slf4j.investment.quant.live.future;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.Future;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.StrategyType;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.live.LiveProcessor;

@Component
public class FutureConsumer implements MessageListener {
	private static final Logger log = Logger.getLogger(FutureConsumer.class);
	
	@Value(value = "${future.activemq.topic}")
	private String topic;
	
	@Autowired
	private LiveProcessor liveProcess;

	@SuppressWarnings("unchecked")
	@Override
	@JmsListener(destination = "${future.activemq.topic}")
	public void onMessage(Message message) {
		List<Timeseries> tsList = null;
		try {
			ObjectMessage objectMessage = (ObjectMessage) message;
			tsList = (List<Timeseries>) objectMessage.getObject();
		} catch (Exception e) {
			log.error("Error during receiving message from the topic:" + topic, e);
		}
		Map<Security, Contract> contractMap = new HashMap<>();
		tsList.stream().forEach(ts -> {
			Variety variety = Variety.of(ts.getVariety());
			Security security = new Security(ts.getSecurity(), variety);
			contractMap.put(security, new Future(ts));
			log.info("Receive:" + ts);
		});
		liveProcess.handle(StrategyType.FUTURE, contractMap);
	}
}