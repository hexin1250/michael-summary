package michael.slf4j.investment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class LangChain4jConfig {

	@Bean("deepSeekProModel")
	public ChatLanguageModel deepSeekProModel() {
		return OpenAiChatModel.builder().apiKey(System.getenv("DEEPSEEK_API_KEY")) // 从环境变量获取
				.modelName("deepseek-v4-pro").baseUrl("https://api.deepseek.com").temperature(0.2)
				.timeout(java.time.Duration.ofSeconds(360)).maxTokens(20000).logRequests(false).logResponses(false).build();
	}
	
	@Bean("deepSeekFlashModel")
	public ChatLanguageModel deepSeekFlashModel() {
		return OpenAiChatModel.builder().apiKey(System.getenv("DEEPSEEK_API_KEY")) // 从环境变量获取
				.modelName("deepseek-v4-flash").baseUrl("https://api.deepseek.com").temperature(1.5)
				.timeout(java.time.Duration.ofSeconds(360)).maxTokens(20000).logRequests(false).logResponses(false).build();
	}
}