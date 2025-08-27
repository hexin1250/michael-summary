package michael.slf4j.investment.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("fileService")
public class FileService {
	private final static String QUESTION_TYPE = "question";
	private final static String ANSWER_TYPE = "answer";

	@Value("${chat.history.folder}")
	private String folderName;
	
	private String tradeDate;
	
	public TreeMap<String, Map<String, File>> getFileStatus() {
		File folder = new File(folderName);
		File[] files = folder.listFiles();
		TreeMap<String, Map<String, File>> map = new TreeMap<>();
		for (File file : files) {
			String name = file.getName();
			String[] parts = name.split("[.]");
			Map<String, File> typeMap = map.get(parts[0]);
			if (typeMap == null) {
				typeMap = new HashMap<>();
				map.put(parts[0], typeMap);
			}
			if (parts[1].contains("question")) {
				typeMap.put(QUESTION_TYPE, file);
			} else {
				typeMap.put(ANSWER_TYPE, file);
			}
		}
		tradeDate = map.lastKey();
		return map;
	}

	public String getTradeDate() {
		return tradeDate;
	}
	
	public String getQuestionFileName() {
		return folderName + "/" + tradeDate + ".question.txt";
	}
	
	public String getAnswerFileName() {
		return folderName + "/" + tradeDate + ".answer.txt";
	}
	
	public String getLatestAnswerFileName() {
		TreeMap<String, Map<String, File>> statusMap = getFileStatus();
		String tradeDate = statusMap.lastKey();
		Map<String, File> typeMap = statusMap.get(tradeDate);
		if(typeMap.containsKey(ANSWER_TYPE)) {
			return typeMap.get(ANSWER_TYPE).getAbsolutePath();
		}
		tradeDate = statusMap.lowerKey(tradeDate);
		typeMap = statusMap.get(tradeDate);
		return typeMap.get(ANSWER_TYPE).getAbsolutePath();
	}

}
