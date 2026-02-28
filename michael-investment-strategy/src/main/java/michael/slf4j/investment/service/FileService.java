package michael.slf4j.investment.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import michael.slf4j.investment.model.Variety;

@Service("fileService")
public class FileService {
	private static final Logger log = Logger.getLogger(FileService.class);
	
	private final static String QUESTION_TYPE = "question";
	private final static String ANSWER_TYPE = "answer";

	@Value("${chat.history.folder}")
	private String historyFolderName;
	
	@Value("${chat.backup.folder}")
	private String backUpfolderName;
	
	@Value(value = "${chat.research.folder}")
	private String researchFolder;
	
	private Map<Variety, String> tradeDateMap = new HashMap<>();
	
	public TreeMap<String, Map<String, File>> getFileStatus(Variety variety) {
		File folder = new File(historyFolderName + "/" + variety.name());
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
			tradeDateMap.put(variety, map.lastKey());
		}
		return map;
	}

	public String getTradeDate(Variety variety) {
		return tradeDateMap.get(variety);
	}
	
	public String getQuestionFileName(String varietyStr) {
		String folder = historyFolderName + "/" + varietyStr;
		File dir = new File(folder);
		List<String> fileNameList = Arrays.stream(dir.list()).filter(fileName -> fileName.endsWith(".question.txt"))
			.sorted((a, b) -> a.compareTo(b) * -1).collect(Collectors.toList());
		return folder + "/" + fileNameList.get(0);
	}
	
	public String getAnswerFileName(String varietyStr) {
		Variety variety = Variety.of(varietyStr);
		String tradeDate = tradeDateMap.get(variety);
		return historyFolderName + "/" + varietyStr + "/" + tradeDate + ".answer.txt";
	}
	
	public String getLatestAnswerFileName(Variety variety) {
		TreeMap<String, Map<String, File>> statusMap = getFileStatus(variety);
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
		housekeepVariety(Variety.I);
		housekeepVariety(Variety.RB);
	}
	
	private void housekeepVariety(Variety variety) {
		File folder = new File(historyFolderName + "/" + variety.name());
		File[] files = folder.listFiles();
		long count = Arrays.stream(files).filter(file -> file.getName().contains("answer.txt")).count();
		if (count == 6) {
			for (File from : files) {
				String targetFileName = backUpfolderName + "/" + variety.name() + "/" + from.getName();
				File to = new File(targetFileName);
				try {
					Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					log.warn("Move file error [" + from.getAbsolutePath() + "]", e);
				}
			}
		}

		File realtimeDir = new File(researchFolder);
		if (realtimeDir.exists() && realtimeDir.isDirectory()) {
			try {
				Files.walk(realtimeDir.toPath()).skip(1).sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
