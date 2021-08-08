package com.pinggao.sequence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.pinggao.sequence.api.SequenceRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceRangeManager implements SequenceRange {
    private final static Logger logger = LoggerFactory.getLogger(SequenceRangeManager.class);
    private SequenceRange currentRange;
    private final int innerStep = 50;
    private int outStep;
    private final String app;
    private final Lock lock = new ReentrantLock();
    private final List<SequenceDao> sequenceDaoList = new ArrayList<>();

    public SequenceRangeManager(String app) {
        this.app = app;
        init();
    }

    void init() {
        initSequenceDao();
        initOutStep();
        currentRange = getNextSequenceRange();
    }

    private void initSequenceDao() {
        String[] dbUrls = {
            "jdbc:mysql://localhost:3306/id_table?useSSL=false",
            "jdbc:mysql://localhost:3306/id_table_slave?useSSL=false"
        };
        String JDBC_USER = "root";
        String JDBC_PASSWORD = null;

        for (String dbUrl : dbUrls) {
            SequenceDao sequenceDaoInstance = getSequenceDaoInstance(dbUrl, JDBC_USER, JDBC_PASSWORD);
            if (sequenceDaoInstance != null) {
                sequenceDaoInstance.createTable();
                sequenceDaoList.add(sequenceDaoInstance);
            }
        }
    }

    private void initOutStep() {
        outStep = innerStep * sequenceDaoList.size();
    }

    private SequenceDao getSequenceDaoInstance(String dbUrl, String user, String password) {
        try {
            Connection connection = DriverManager.getConnection(dbUrl, user, password);
            return new SequenceDao(connection, app);
        } catch (SQLException throwable) {
            logger.error("sequenceDao初始化失败,dbUrl:{} user:{}", dbUrl, user);
        }
        return null;
    }

    @Override
    public long nextValue() {
        final SequenceRange oldRange = this.getCurrentRange();
        if (oldRange == null) {
            logger.error("SequenceRangeManager持有的SequenceRange为空,超出预期");
            return -1L;
        }

        long id = oldRange.nextValue();
        if (id == -1L) {
            if (oldRange.over()) {
                //换下个
                lock.lock();
                try {
                    if (this.getCurrentRange() == oldRange) {
                        this.currentRange = getNextSequenceRange();

                        // TODO: 2021/7/21 数据源全挂了
                        if (this.currentRange == null) {
                            throw new RuntimeException("无法生成SequenceRange....异常退出");
                        }
                    }
                    id = this.currentRange.nextValue();
                } finally {
                    lock.unlock();
                }
            } else {
                logger.warn("SequenceRange is not over , but cant generate nextValue");
            }
        }
        return id;
    }

    @Override
    public boolean over() {
        return false;
    }

    private SequenceRange getNextSequenceRange() {
        if (sequenceDaoList.isEmpty()) {
            throw new RuntimeException("数据源为空,无法进行position持久化");
        }

        //得到一个打乱的数组用来随机选择数据库
        List<Integer> dsIndex = IntStream.range(0, sequenceDaoList.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(dsIndex);

        int retryTime = 3;

        while (true) {
            if (retryTime < 0) {
                logger.error("尝试链接数据达到重试上限,退出");
                break;
            }

            for (Integer id : dsIndex) {
                try {
                    SequenceDao sequenceDao = sequenceDaoList.get(id);
                    logger.info("随机选取数据源:{}", id);
                    long oldValue = sequenceDao.getOldValue();
                    if (oldValue == -1L) {
                        //可能没有行,尝试创建第一行
                        oldValue = insertFirstLine(id, sequenceDao);
                    }

                    long newValue = oldValue + outStep;

                    //乐观锁更新
                    boolean updateResult = sequenceDao.updateValue(oldValue, newValue);
                    if (updateResult) {
                        logger.info("乐观锁更新成功 app: {} oldValue:{} ---> newValue:{}", app, oldValue, newValue);
                        return new SequenceRangeImpl(oldValue, oldValue + innerStep);
                    } else {
                        throw new RuntimeException("乐观锁更新失败");
                    }
                } catch (RuntimeException e) {
                    logger.warn("尝试在 {} 号数据源进行position持久化失败", id);
                }
            }
            retryTime--;
        }
        return null;
    }

    private long insertFirstLine(Integer id, SequenceDao sequenceDao) {
        long oldValue;
        long startPosition = id * innerStep;
        boolean insertFlag = sequenceDao.insertPosition(startPosition);
        if (insertFlag) {
            logger.info("插入第一行成功  app:{}  startPosition:{}", app, startPosition);
            oldValue = startPosition;
        } else {
            logger.warn("插入第一行失败 数据源:{}", sequenceDao);
            throw new RuntimeException("数据源插入失败 ");
        }
        return oldValue;
    }

    public SequenceRange getCurrentRange() {
        return currentRange;
    }
}
