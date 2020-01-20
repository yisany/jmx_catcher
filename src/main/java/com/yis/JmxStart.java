package com.yis;

import com.alibaba.fastjson.JSON;
import com.yis.exporter.BuildInfoCollector;
import com.yis.exporter.JmxCollector;
import com.yis.task.ScraperTask;
import com.yis.kafka.KafkaHelper;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author milu
 * @Description jmx 性能监控
 * @createTime 2019年09月02日 15:53:00
 */
public class JmxStart {

    public final static Logger logger = LoggerFactory.getLogger(JmxStart.class);

    /**
     * 数据展示url,有多少个host就要有多少个url
     */
    public static String url = "127.0.0.1";

    /**
     * 是否开启网页展示
     */
    public static boolean openUrl = true;

    /**
     * 数据展示端口
     */
    public static Integer[] urlPorts;

    /**
     * 初始端口
     */
    public static int initPort = 23333;

    /**
     * 采集间隔,单位min
     */
    public static int interval = 1;

    /**
     * 存放jmxurl,中间用,分隔
     */
    public String hosts = "172.16.8.164:1099";

    public List<String> hostList;

    /**
     * 解析规则,具体可以看jmx_exporter
     */
    public Map<String, Object> confs;

    /**
     * 决定开启多少线程
     */
    public int number;

    /**
     * Quartz计划任务
     */
    public Scheduler scheduler;

    public ExecutorService pool;

    public AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {
        logger.info("jmx_catcher is starting...");
        JmxStart jmx = new JmxStart();
        jmx.prepare();
        jmx.emit();
        jmx.release();
    }

    private void prepare() {
        // init config
        initConfig();

        try {
            StdSchedulerFactory sf = new StdSchedulerFactory();
            sf.initialize(this.getClass().getClassLoader().getResourceAsStream("jmx_quartz.properties"));
            scheduler = sf.getScheduler();
            scheduler.start();

            pool = Executors.newFixedThreadPool(number);
        } catch (SchedulerException e) {
            logger.error("create QuartzScheduler error, error: {}", e);
        }
        logger.info("jmx_catcher.prepare in finished...");
    }

    private void emit() {
        logger.info("jmx_catcher is working...");
        for (String host : hostList) {
            try {
                if (openUrl) {
                    pool.execute(() -> {
                        try {
                            InetSocketAddress socket = new InetSocketAddress(JmxStart.url, JmxStart.urlPorts[count.getAndIncrement()]);

                            new BuildInfoCollector().register();
                            new JmxCollector(host, confs).register();
                            new HTTPServer(socket, CollectorRegistry.defaultRegistry);
                        } catch (MalformedObjectNameException e) {
                            logger.error("jmxStart.JmxCollector error, error: {}", e);
                        } catch (IOException e) {
                            logger.error("jmxStart.JmxCollector error, error: {}", e);
                        }
                    });
                } else {
                    JobDetail jobDetail = JobBuilder.newJob(ScraperTask.class)
                            .withIdentity(host, "jmx")
                            .usingJobData("host", host)
                            .usingJobData("config", JSON.toJSONString(confs))
                            .build();

                    SimpleTrigger trigger = TriggerBuilder.newTrigger()
                            .startNow()
                            .withIdentity(host, "jmx")
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInMinutes(interval)
                                    .withRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY))
                            .build();

                    scheduler.scheduleJob(jobDetail, trigger);
                }
            } catch (SchedulerException e) {
                logger.error("jmxStart.startQuartz error, error: {}", e);
            }
        }
    }

    /**
     * 关闭
     */
    private void release() {
//        try {
//            if (!scheduler.isShutdown()) {
//                scheduler.shutdown();
//            }
//            KafkaHelper.getKafkaInstance().release();
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
    }

    private void initConfig() {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(this.getClass().getClassLoader().getResourceAsStream("application.yaml"));
        Map<String, Object> monitorConfig = (Map<String, Object>) config.get("monitor");
        Map<String, Object> showConfig = (Map<String, Object>) config.get("show");


        interval = (int) monitorConfig.get("interval");
        hosts = (String) monitorConfig.get("hosts");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) monitorConfig.get("rules");
        confs = new HashMap() {{
            put("lowercaseOutputLabelNames", true);
            put("lowercaseOutputName", true);
            put("rules", rules);
        }};

        openUrl = (boolean) showConfig.get("openUrl");
        if (openUrl) {
            url = (String) showConfig.get("url");
            initPort = (int) showConfig.get("initPort");
        } else {
            Map<String, Object> kafkaConfig = (Map<String, Object>) config.get("kafka");
            KafkaHelper
                    .initKafkaInstance((String) kafkaConfig.get("bootstrapServers"), (String) kafkaConfig.get("topic"))
                    .prepare();
        }

        hostList = Arrays.asList(hosts.split(","));
        number = hostList.size();


        urlPorts = new Integer[number];
        for (int i = 0; i < number; i++) {
            urlPorts[i] = initPort++;
        }
    }
}
