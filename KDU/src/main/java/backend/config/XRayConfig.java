package backend.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ElasticBeanstalkPlugin;
import com.amazonaws.xray.strategy.sampling.DefaultSamplingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import com.amazonaws.xray.strategy.sampling.SamplingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.Filter;
import java.net.URL;

@Configuration
public class XRayConfig {
    private static final Logger logger = LoggerFactory.getLogger(XRayConfig.class);

    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public Filter xrayFilter() {
        logger.info("Registering AWS X-Ray Servlet Filter for service: {}", serviceName);
        return new AWSXRayServletFilter(serviceName);
    }

    static {
        try {
            logger.info("Initializing AWS X-Ray configuration");

            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                    .withPlugin(new EC2Plugin())
                    .withPlugin(new ElasticBeanstalkPlugin());

            logger.info("AWS X-Ray plugins registered: EC2Plugin and ElasticBeanstalkPlugin");

            URL samplingRules = XRayConfig.class.getResource("/sampling-rules.json");
            SamplingStrategy samplingStrategy;

            if (samplingRules != null) {
                try {
                    samplingStrategy = new LocalizedSamplingStrategy(samplingRules);
                    logger.info("Successfully loaded custom sampling rules from sampling-rules.json");
                } catch (Exception e) {
                    logger.warn("Failed to load custom sampling rules, falling back to default", e);
                    samplingStrategy = new DefaultSamplingStrategy();
                }
            } else {
                logger.info("No sampling rules file found, using default sampling strategy");
                samplingStrategy = new DefaultSamplingStrategy();
            }

            builder.withSamplingStrategy(samplingStrategy);
            AWSXRay.setGlobalRecorder(builder.build());
            logger.info("AWS X-Ray configuration completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize AWS X-Ray configuration", e);
            throw new RuntimeException("Failed to initialize AWS X-Ray configuration", e);
        }
    }
}
