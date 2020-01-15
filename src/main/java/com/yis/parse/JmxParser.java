package com.yis.parse;

import com.alibaba.fastjson.JSON;
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
    public final static FastDateFormat TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private List<Collector.MetricFamilySamples> familySamples;

    public JmxParser(List<Collector.MetricFamilySamples> familySamples) {
        this.familySamples = familySamples;
    }

    public void handle() {
        String timestamp = getEsTime();

        for (Collector.MetricFamilySamples fs : familySamples) {
            Map<String, Object> res = new HashMap<>();
            String name = fs.name;
            Collector.Type type = fs.type;

            List<Map<String, Object>> m = new ArrayList<>();
            for (Collector.MetricFamilySamples.Sample metric : fs.samples) {
                Map<String, Object> ms = new HashMap<>();
                ms.put("name", metric.name);
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
                int size = Integer.max(labelNames.size(), labelValues.size());
                for (int i = 0; i < size; i++) {
                    ms.put(labelNames.get(i), labelValues.get(i));
                }
                m.add(ms);
            }
            res.put("name", name);
            res.put("type", type);
            res.put("metrics", m);
            res.put("timestamp", timestamp);

            // 发送到kafka
            KafkaHelper.getKafkaInstance().pushToKafka(JSON.toJSONString(res));

        }
    }

    /**
     * 获取ES支持的UTC时间
     *
     * @return
     */
    public static String getEsTime() {
        Date date = new Date();
        return TIME_FORMAT.format(date.getTime() - 8 * 60 * 60 * 1000);
    }

}
