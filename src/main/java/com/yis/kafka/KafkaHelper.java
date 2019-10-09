package com.yis.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

/**
 * @author milu
 * @Description kafka工具类
 * @createTime 2019年08月22日 19:11:00
 */
public class KafkaHelper {

    private final static Logger log = LoggerFactory.getLogger(KafkaHelper.class);

    private Properties props;
    private static JKafkaProducer producer;
    private String bootstrapServers;
    private String topic;

    private static volatile KafkaHelper kafkaHelper;

    private KafkaHelper(String bootstrapServers, String topic) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    public static KafkaHelper getKafkaInstance() {
        if (kafkaHelper == null) {
            synchronized (KafkaHelper.class) {
                if (kafkaHelper == null) {
                    throw new RuntimeException("kafka没有初始化, 请运行initKafkaInstance(String bootstrapServers, String topic)");
                }
            }
        }
        return kafkaHelper;
    }

    public static KafkaHelper initKafkaInstance(String bootstrapServers, String topic) {
        if (kafkaHelper == null) {
            synchronized (KafkaHelper.class) {
                if (kafkaHelper == null) {
                    kafkaHelper = new KafkaHelper(bootstrapServers, topic);
                }
            }
        }
        return kafkaHelper;
    }

    public void prepare() {
        try {
            if (props == null) {
                props = new Properties();
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            }
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            producer = JKafkaProducer.init(props);

        } catch (Exception e) {
            log.error("kafka producer init error", e);
            throw new RuntimeException("kafka producer init error");
        }
    }

    public void release() {
        producer.close();
        log.info("kafka producer release.");
    }

    public void pushToKafka(String msg) {
        producer.sendWithRetry(topic, UUID.randomUUID().toString(), msg);
    }


}
