package com.yis.task;

import com.alibaba.fastjson.JSON;
import com.yis.exporter.JmxCollector;
import com.yis.parse.JmxParser;
import io.prometheus.client.Collector;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import java.util.List;
import java.util.Map;

/**
 * @author milu
 * @Description job
 * @createTime 2019年09月03日 17:19:00
 */
public class ScraperTask implements Job {

    private final static Logger logger = LoggerFactory.getLogger(ScraperTask.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String host = jobDataMap.getString("host");
        Map<String, Object> config = JSON.parseObject(jobDataMap.getString("config"), Map.class);

        try {
            List<Collector.MetricFamilySamples> familySamples = new JmxCollector(host, config).collect();

            JmxParser parser = new JmxParser(familySamples);
            parser.handle();
        } catch (MalformedObjectNameException e) {
            logger.error("ScraperTask.JmxCollector error, error:{}", e);
        }
    }
}
