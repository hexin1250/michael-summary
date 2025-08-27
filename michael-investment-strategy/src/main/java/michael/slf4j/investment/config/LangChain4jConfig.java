package michael.slf4j.investment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class LangChain4jConfig {

	@Bean
	public ChatLanguageModel deepSeekChatModel() {
		return OpenAiChatModel.builder().apiKey(System.getenv("DEEPSEEK_API_KEY")) // 从环境变量获取
				.modelName("deepseek-reasoner").baseUrl("https://api.deepseek.com").temperature(1.3)
				.timeout(java.time.Duration.ofSeconds(120)).maxTokens(65536).logRequests(false).logResponses(false).build();
	}
}