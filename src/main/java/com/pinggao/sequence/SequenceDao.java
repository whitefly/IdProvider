package com.pinggao.sequence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhouang
 */
public class SequenceDao {
    private static final Logger logger = LoggerFactory.getLogger(SequenceDao.class);
    final static String JDBC_URL = "jdbc:mysql://localhost:3306/id_table?useSSL=false";
    final static String JDBC_USER = "root";
    final static String JDBC_PASSWORD = null;
    private Connection conn = null;

    final static String TABLE_NAME = "ds1";
    final static String APP_COLUMN_NAME = "app_name";
    final static String POSITION_COLUMN_NAME = "position";
    private String appName = null;

    public SequenceDao(Connection conn, String appName) {
        this.conn = conn;
        this.appName = appName;
    }

    public long getOldValue() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "select " + POSITION_COLUMN_NAME + " from " + TABLE_NAME + " where " + APP_COLUMN_NAME + " = ?");
            ps.setString(1, appName);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(POSITION_COLUMN_NAME);
            } else {
                //对应行没有创建
                return -1L;
            }
        } catch (SQLException throwables) {
            logger.error("sql执行错误", throwables);
        }
        return -1L;
    }

    /**
     * 乐观锁更新数据库
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    public boolean updateValue(long oldValue, long newValue) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "update " + TABLE_NAME + " set " + POSITION_COLUMN_NAME + " = ? where " + APP_COLUMN_NAME +
                    " = ? and " + POSITION_COLUMN_NAME + " = ?");
            ps.setLong(1, newValue);
            ps.setString(2, appName);
            ps.setLong(3, oldValue);
            return ps.executeUpdate() > 0;
        } catch (SQLException throwables) {
            logger.error("更新数据库失败 app:{} oldValue:{} newValue:{}", appName, oldValue, newValue);
        }
        return false;
    }

    public boolean insertPosition(long initPosition) {
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(
                "insert into " + TABLE_NAME + " ( " + APP_COLUMN_NAME + ", " + POSITION_COLUMN_NAME
                    + " ) values (?, ?)");
            preparedStatement.setString(1, appName);
            preparedStatement.setLong(2, initPosition);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException throwables) {
            logger.error("插入数据库失败,app:{} position:{}", appName, initPosition);
            return false;
        }
    }

    public boolean createTable() {
        try {
            PreparedStatement preparedStatement = conn.prepareStatement("create TABLE if not exists " + TABLE_NAME
                + "("
                + "    id       int auto_increment,"
                + "    app_name varchar(50) not null unique,"
                + "    position int         not null,"
                + "    primary key (id)"
                + ") ENGINE = InnoDB"
                + "  default CHARSET = utf8;");
            int i = preparedStatement.executeUpdate();
            return i == 0;
        } catch (SQLException e) {
            logger.error("创建position表失败", e);
            return false;
        }
    }

    public static void main(String[] args) throws SQLException {
        //SequenceDao sequenceDao = new SequenceDao();
        //System.out.println(sequenceDao.createTable());
        logger.info("你好");
    }

}
