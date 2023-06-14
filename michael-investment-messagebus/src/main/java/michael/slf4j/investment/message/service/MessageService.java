package michael.slf4j.investment.message.service;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

@Component("messageService")
public class MessageService {
	private static final Logger log = Logger.getLogger(MessageService.class);
	
	@Autowired
	private JmsMessagingTemplate jmsTemplate;

	public void send(String topicName, Object content) throws JMSException {
		log.info("Send message:" + content);
		jmsTemplate.convertAndSend(topicName, content);
	}

}