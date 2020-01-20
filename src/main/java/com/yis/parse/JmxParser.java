package com.yis.parse;

import com.yis.kafka.KafkaHelper;
import io.prometheus.client.Collector;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author milu
 * @Description jmx解析
 * @createTime 2019年09月09日 15:14:00
 */
public class JmxParser {

    private final static Logger logger = LoggerFactory.getLogger(JmxParser.class);

    private List<Collector.MetricFamilySamples> familySamples;
    private String host;
    private String timestamp;

    public JmxParser(List<Collector.MetricFamilySamples> familySamples, String host, String timestamp) {
        this.familySamples = familySamples;
        this.host = host;
        this.timestamp = timestamp;
    }

    public void handle() {
        List<Map<String, Object>> m = new ArrayList<>();
        for (Collector.MetricFamilySamples fs : familySamples) {
            String name = fs.name;
            Collector.Type type = fs.type;

            for (Collector.MetricFamilySamples.Sample metric : fs.samples) {
                Map<String, Object> ms = new HashMap<>();
                ms.put("name", name);
                ms.put("type", type);
                ms.put("metricName", metric.name);
                ms.put("value", metric.value);
//                switch (fs.type) {
//                    case HISTOGRAM:
//                        break;
//                    case COUNTER:
//                        break;
//                    case GAUGE:
//
//                        break;
//                    case SUMMARY:
//                        break;
//                    case UNTYPED:
//                        break;
//                    default:
//                        break;
//                }
                List<String> labelNames = metric.labelNames;
                List<String> labelValues = metric.labelValues;
                int size = Integer.min(labelNames.size(), labelValues.size());
                for (int i = 0; i < size; i++) {
                    ms.put(labelNames.get(i), labelValues.get(i));
                }
                // 发送到kafka

                ms.put("host", host);
                ms.put("timestamp", timestamp);
                m.add(ms);
                KafkaHelper.getKafkaInstance().pushToKafka(ms);
            }
        }
        logger.info("ok, host={}, timestamp={}, size={}", host, timestamp, m.size());
    }

}
