### 分布式id生成器 

看了阿里开源的TDDL数据库中间中的某模块后,自己的仿写版

1. 本质原理就是的等差数列,每个SequenceRange事先分配范围
2. 多数据源,失败有重试机制
3. 仅仅一个暂时原理的小Demo



#### 功能说明 
    1.SequenceDao类主要负责持久化id范围,生成SequenceRange
    2.SequenceRangeManager负责整体调度