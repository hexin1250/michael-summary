package michael.slf4j.investment.service;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import michael.slf4j.investment.model.Variety;

@Service("statelessChatService")
public class StatelessChatService {
	private final static Logger log = Logger.getLogger(StatelessChatService.class);
	
	private final static String QUESTION_TYPE = "question";
	private final static String ANSWER_TYPE = "answer";

	@Value("${chat.history.folder}")
	private String folderName;
	
	@Value("${chat.point.folder}")
	private String pointFolderName;
	
	@Autowired
	private FileService fileService;

	private final ChatLanguageModel chatModel;

	public StatelessChatService(ChatLanguageModel chatModel) {
		this.chatModel = chatModel;
	}

	public void doResearch(Variety variety) throws FileNotFoundException, IOException {
		log.info("Start to do research through Deepseek for " + variety.name());
		Map<String, Map<String, File>> map = fileService.getFileStatus(variety);
		int size = map.size();
		int start = size - 10;
		int count = 0;
		String tradeDate = null;

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from("你是一个专业的期货投资顾问,擅长技术分析和解释市场趋势.我是激进投资者,我只会100%仓位操作.用中文回答。"));
		for (Entry<String, Map<String, File>> entry : map.entrySet()) {
			count++;
			if (count <= start) {
				continue;
			}
			Map<String, File> typeMap = entry.getValue();
			File questionFile = typeMap.get(QUESTION_TYPE);
			String question = getContent(questionFile);
			messages.add(UserMessage.from(question));

			if (count != size) {
				File answerFile = typeMap.get(ANSWER_TYPE);
				String answer = getContent(answerFile);
				messages.add(AiMessage.from(answer));
			}
			tradeDate = entry.getKey();
		}

		// 调用模型生成回复
		Response<AiMessage> aiReply = chatModel.generate(messages);
		String reply = aiReply.content().text();
		writeFile(variety.name(), tradeDate, reply);
		
		log.info("Done to get answer");
	}

	private String getContent(File file) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		}
	}
	
	private void writeFile(String variety, String tradeDate, String content) throws FileNotFoundException, IOException {
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileService.getAnswerFileName(variety))))){
			bw.write(content);
			bw.newLine();
			bw.flush();
		}
	}
	
}