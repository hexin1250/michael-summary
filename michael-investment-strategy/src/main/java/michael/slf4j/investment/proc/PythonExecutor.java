package michael.slf4j.investment.proc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class PythonExecutor {
	private static final Logger log = Logger.getLogger(PythonExecutor.class);

	/**
	 * 执行 Python 脚本
	 * 
	 * @param pythonPath Python解释器路径（如："python" 或 "C:/Python39/python.exe"）
	 * @param scriptPath Python脚本路径
	 * @param args       传递给脚本的参数
	 * @return 执行结果（0为成功，非0为失败）
	 */
	public static int executePython() {
		log.info("Waiting 10 seconds...");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}
		log.info("Start Deepseek");
		
		String pythonPath = "C:\\Program Files\\Python310\\python.exe"; // 或指定完整路径如 "/usr/bin/python3"
		String scriptPath = "C:\\Users\\HP\\python-workspace\\myproject\\main2.py";
		try {
			// 构建命令参数
			String[] cmd = new String[2];
			cmd[0] = pythonPath;
			cmd[1] = scriptPath;

			// 创建进程
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true); // 合并标准错误和标准输出

			// 启动进程
			Process process = pb.start();

			// 异步读取输出（防止阻塞）
			Thread outputThread = new Thread(() -> {
				try (InputStream is = process.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
					while (reader.readLine() != null) {
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			outputThread.start();

			// 等待执行完成
			int exitCode = process.waitFor();
			outputThread.join(); // 确保输出读取完成
			log.info("Deepseek research is done");

			return exitCode;

		} catch (IOException | InterruptedException e) {
			log.error("执行失败: " + e.getMessage());
			return -1;
		}
	}

	public static void main(String[] args) {
		int result = executePython();

		System.out.println("执行结果代码: " + result);
	}
}