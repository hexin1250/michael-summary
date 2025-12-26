package michael.slf4j.investment.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

@Service("fileService")
public class FileService {
	private static final Logger log = Logger.getLogger(FileService.class);
	
	private final static String QUESTION_TYPE = "question";
	private final static String ANSWER_TYPE = "answer";

	@Value("${chat.history.folder}")
	private String historyFolderName;
	
	@Value("${chat.backup.folder}")
	private String backUpfolderName;
	
	private String tradeDate;
	
	public TreeMap<String, Map<String, File>> getFileStatus() {
		File folder = new File(historyFolderName);
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
		if(!map.isEmpty()) {
			tradeDate = map.lastKey();
		}
		return map;
	}

	public String getTradeDate() {
		return tradeDate;
	}
	
	public String getQuestionFileName() {
		return historyFolderName + "/" + tradeDate + ".question.txt";
	}
	
	public String getAnswerFileName() {
		return historyFolderName + "/" + tradeDate + ".answer.txt";
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
	
	public boolean fullRequired(String tradeDate) {
		File folder = new File(historyFolderName);
		File[] files = folder.listFiles();
		long count = Arrays.stream(files).filter(file -> !file.getName().contains(tradeDate)).count();
		return count == 0L;
	}
	
	public void housekeeping() {
		File folder = new File(historyFolderName);
		File[] files = folder.listFiles();
		long count = Arrays.stream(files).filter(file -> file.getName().contains("answer.txt")).count();
		if(count == 6) {
			for (File from : files) {
				String targetFileName = backUpfolderName + "/" + from.getName();
				File to = new File(targetFileName);
				try {
					Files.move(from, to);
				} catch (IOException e) {
					log.warn("Move file error [" + from.getAbsolutePath() + "]", e);
				}
			}
		}
	}

}
