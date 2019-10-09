package com.yis.parse;

import com.alibaba.fastjson.JSONObject;
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
        List<String> msgs = new ArrayList<>();
        String timestamp = getEsTime();
        for (Collector.MetricFamilySamples familySample : familySamples) {
            JSONObject obj = new JSONObject();

            List<JSONObject> samples = new ArrayList<>();
            for (Collector.MetricFamilySamples.Sample sample : familySample.samples) {
                Map<String, Object> label = new HashMap<>();
                JSONObject sam = new JSONObject();
                for (int i = 0; i < sample.labelNames.size(); i++) {
                    label.put(sample.labelNames.get(i), sample.labelValues.get(i));
                }
                sam.put("name", sample.name);
                sam.put("value", sample.value);
                sam.put("label", label);
                samples.add(sam);
            }

            obj.put("timestamp", timestamp);
            obj.put("name", familySample.name);
            obj.put("type", familySample.type);
            obj.put("samples", samples);
            msgs.add(obj.toJSONString());
        }
        for (String msg : msgs) {
            KafkaHelper.getKafkaInstance().pushToKafka(msg);
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
