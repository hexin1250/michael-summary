package michael.slf4j.investment.research;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

@Component("myChat")
public class MyChatBot {
	private static final Logger log = Logger.getLogger(MyChatBot.class);
	private static final Pattern p = Pattern.compile("(.*)[.](.*)[.](.*)");
	private String folderName = "src/main/data/custom";
	
	@Autowired
	@Qualifier("deepSeekFlashModel")
	private ChatLanguageModel chatModel;
	
	public String resultPath() throws FileNotFoundException, IOException {
		File dir = new File(folderName);
		File[] files = dir.listFiles();
		Arrays.sort(files, (a, b) -> {
			String fileNameA = a.getName();
			String fileNameB = b.getName();
			Matcher matcherA = p.matcher(fileNameA);
			Matcher matcherB = p.matcher(fileNameB);
			if(!matcherA.matches()) {
				return -1;
			}
			if(!matcherB.matches()) {
				return 1;
			}
			String numberPartA = matcherA.group(2);
			String typePartA = matcherA.group(1);
			String numberPartB = matcherB.group(2);
			int numberA = Integer.valueOf(numberPartA);
			int numberB = Integer.valueOf(numberPartB);
			if(numberA - numberB != 0) {
				return numberA - numberB;
			}
			if(typePartA.equals("question")) {
				return -1;
			} else {
				return 1;
			}
		});
		List<ChatMessage> messages = new ArrayList<>();
		int index = 0;
		boolean close = false;
		for (File file : files) {
			String fileName = file.getName();
			Matcher matcher = p.matcher(fileName);
			if(!matcher.matches()) {
				log.error("What's the file name???" + fileName);
				break;
			}
			String numberPart = matcher.group(2);
			String typePart = matcher.group(1);
			index = Integer.valueOf(numberPart);
			if("question".equalsIgnoreCase(typePart)) {
				close = false;
				messages.add(UserMessage.from(getContent(folderName + "/" + fileName)));
			} else {
				close = true;
				messages.add(AiMessage.from(getContent(folderName + "/" + fileName)));
			}
		}
		String ret = folderName + "/answer." + index + ".txt";
		if(close) {
			log.info("This is the latest response:" + ret);
			return ret;
		}
		Response<AiMessage> contextAiReply = chatModel.generate(messages);
		String contextReply = contextAiReply.content().text();
		writeFile(ret, contextReply);
		return ret;
	}
	
	private String getContent(String fileName) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))) {
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		}
	}
	
	private void writeFile(String fileName, String content) throws FileNotFoundException, IOException {
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))){
			bw.write(content);
			bw.newLine();
			bw.flush();
		}
	}

}
