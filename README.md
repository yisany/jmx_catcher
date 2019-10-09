# jmx_catcher

### Monitor

基于prometheus的jmx_exporter插件改造

- 去除原来的`-javaagent`方式启动

- 改用java进程自行开启jmx功能, 监听ip:port的形式

原来的agent嵌入式方案在使用时感到有些不便, 每一个进程启动就需要重新配一次插件. 
但现在的方案也有缺点, 会暴露端口.

将需要采集进程的host地址写入`application.yaml`文件的host项, 中间用逗号分隔. 
具体的采集规则可以在`rules`项进行配置.

Name     | Description
---------|------------
rules    | A list of rules to apply in order, processing stops at the first matching rule. Attributes that aren't matched aren't collected. If not specified, defaults to collecting everything in the default format.
pattern  | Regex pattern to match against each bean attribute. The pattern is not anchored. Capture groups can be used in other options. Defaults to matching everything.
attrNameSnakeCase | Converts the attribute name to snake case. This is seen in the names matched by the pattern and the default format. For example, anAttrName to an\_attr\_name. Defaults to false.
name     | The metric name to set. Capture groups from the `pattern` can be used. If not specified, the default format will be used. If it evaluates to empty, processing of this attribute stops with no output.
value    | Value for the metric. Static values and capture groups from the `pattern` can be used. If not specified the scraped mBean value will be used.
valueFactor | Optional number that `value` (or the scraped mBean value if `value` is not specified) is multiplied by, mainly used to convert mBean values from milliseconds to seconds.
labels   | A map of label name to label value pairs. Capture groups from `pattern` can be used in each. `name` must be set to use this. Empty names and values are ignored. If not specified and the default format is not being used, no labels are set.
help     | Help text for the metric. Capture groups from `pattern` can be used. `name` must be set to use this. Defaults to the mBean attribute description and the full name of the attribute.
type     | The type of the metric, can be `GAUGE`, `COUNTER` or `UNTYPED`. `name` must be set to use this. Defaults to `UNTYPED`.

### Show & Collect

设置开关openUrl:

- openUrl: true

  数据采用prometheus规定的格式展示在web页面上, 可直接使用prometheus接入此数据.
  
  默认端口为`23333`, 可配置(initPort). 
  配置一个host会占用一个端口, 有几个host就会有几个页面, 端口号从`initPort`开始递增.

- openUrl: false

  使用此项需要配置`kafka`消息队列. 数据会进行解析, 并进入到kafka供下一个组件消费.
  