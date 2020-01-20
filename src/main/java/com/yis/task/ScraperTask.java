package com.yis.task;

import com.alibaba.fastjson.JSON;
import com.yis.exporter.JmxCollector;
import com.yis.parse.JmxParser;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.commons.lang3.time.FastDateFormat;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import java.util.*;

/**
 * @author milu
 * @Description job
 * @createTime 2019年09月03日 17:19:00
 */
public class ScraperTask implements Job {

    private final static Logger logger = LoggerFactory.getLogger(ScraperTask.class);
    public final static FastDateFormat TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String host = jobDataMap.getString("host");
        String timestamp = getEsTime();
        Map<String, Object> config = JSON.parseObject(jobDataMap.getString("config"), Map.class);
        logger.info("ScraperTask.execute, host={}", host);
        try {
            JmxCollector jmxCollector = new JmxCollector(host, config);
            List<Collector.MetricFamilySamples> familySamples = jmxCollector.collect();

            JmxParser parser = new JmxParser(familySamples, host, timestamp);
            parser.handle();
        } catch (MalformedObjectNameException e) {
            logger.error("ScraperTask.JmxCollector error, error:{}", e);
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
