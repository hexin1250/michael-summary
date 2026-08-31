package michael.slf4j.investment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import michael.slf4j.investment.constant.TopicConstants;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.research.DataResearchV2;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.service.StatelessChatService;

@Service
public class ResearchUtil {

	@Autowired
	private DataResearchV2 dataResearchV2;
	
	@Autowired
	private StatelessChatService statelessChatService;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private MessageService messageService;
	
	@Value("${chat.history.folder}")
	private String historyFolderName;
	
	@Value("${chat.backup.folder}")
	private String backupFolderName;
	
	public void doResearch(Variety variety) throws FileNotFoundException, IOException {
		generateSummary(variety);
		statelessChatService.doResearch(variety);
		sendMessage(variety);
	}
	
	public void doSingleResearch(Variety variety) throws FileNotFoundException, IOException {
		statelessChatService.doSingleResearch(variety);
		sendMessage(variety);
	}
	
	private void generateSummary(Variety variety) {
		long timestamp = TradeUtil.getTradeDate();
		String tradeDate = TradeUtil.getDateStr(timestamp);
		boolean isFullRequired = fileService.fullRequired(tradeDate);
		dataResearchV2.summarize(variety, isFullRequired);
	}
	
	private void sendMessage(Variety variety) {
		File historyFolder = new File(historyFolderName + "/" + variety.name());
		File[] files = historyFolder.listFiles();
		if(files.length == 0) {
			File backupFolder = new File(backupFolderName + "/" + variety.name());
			files = backupFolder.listFiles();
		}
		File file = Arrays.stream(files).filter(a -> a.getName().contains("answer.txt")).max((a, b) -> {
			return a.getName().compareTo(b.getName());
		}).get();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			String line = null;
			int wordCount = 0;
			StringBuffer sb = new StringBuffer();
			while((line = br.readLine()) != null) {
				if(wordCount + line.length() < 1900) {
					sb.append(line);
					sb.append("\n");
					wordCount = wordCount + line.length();
				} else {
					messageService.send(TopicConstants.NOTIFICATION_TOPIC, sb.toString());
					wordCount = line.length();
					sb = new StringBuffer(line);
				}
			}
			if(sb.length() > 0) {
				messageService.send(TopicConstants.NOTIFICATION_TOPIC, sb.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
