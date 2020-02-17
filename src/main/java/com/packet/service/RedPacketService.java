package com.packet.service;

import com.packet.entity.RedPacket;
import com.packet.entity.UserRedPacket;
import com.packet.repository.RedPacketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class RedPacketService {
    @Autowired
    private RedPacketRepository redPacketRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DataSource dataSource;
    private static final String PREFIX = "red_packet_list_";
    private static final int PRE_SIZE = 1000;


    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public RedPacket getRedPacket(Long id){
        return redPacketRepository.getRedPacket(id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int decrRedPacket(Long id){
        return redPacketRepository.decrRedPacket(id);
    }

    /**
     * redis操作
     * @param redPacketId
     * @param unitAmount
     */
    @Async
    public void saveUserRedPacketByRedis(Long redPacketId,Double unitAmount){
        System.out.println("saving start...");
        Long start = System.currentTimeMillis();
        BoundListOperations listOps = redisTemplate.boundListOps(PREFIX + redPacketId);
        Long size = listOps.size();
        Long times = size % PRE_SIZE == 0 ? size / PRE_SIZE : size / PRE_SIZE + 1;
        int count = 0;
        List<UserRedPacket> userRedPackets = new ArrayList<>(PRE_SIZE);
        for (int i = 0; i < times ; i++) {
            List userIds = null;
            if (i == 0){
                userIds = listOps.range(i * PRE_SIZE,(i + 1) * PRE_SIZE);
            }else {
                userIds = listOps.range(i * PRE_SIZE + 1,(i + 1) * PRE_SIZE);
            }

            userRedPackets.clear();
            for (int j = 0; j < userIds.size() ; j++) {
                String args = userIds.get(j).toString();
                String[] arr = args.split("-");
                String userIdStr = arr[0];
                String timeStr = arr[1];
                Long userId = Long.parseLong(userIdStr);
                Long time = Long.parseLong(timeStr);
                // 生成抢红包信息
                UserRedPacket userRedPacket = new UserRedPacket();
                userRedPacket.setRedPacketId(redPacketId);
                userRedPacket.setUserId(userId);
                userRedPacket.setAmount(unitAmount);
                userRedPacket.setGetTime(new Timestamp(time));
                userRedPacket.setNote("抢红包 " + redPacketId);
                userRedPackets.add(userRedPacket);
            }
            // 插入抢红包信息
            count += executeBatch(userRedPackets);
        }
        // 删除Redis列表
        redisTemplate.delete(PREFIX + redPacketId);
        Long end = System.currentTimeMillis();
        System.err.println("保存数据结束，耗时" + (end - start) + "毫秒，共" + count + "条记录被保存。");
    }

    /**
     * 使用JDBC批量处理Redis缓存数据.
     *
     * @param userRedPacketList
     *            -- 抢红包列表
     * @return 抢红包插入数量.
     */
    private int executeBatch(List<UserRedPacket> userRedPacketList) {
        Connection conn = null;
        Statement stmt = null;
        int[] count = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            for (UserRedPacket userRedPacket : userRedPacketList) {
                String sql1 = "update T_RED_PACKET set stock = stock-1 where id=" + userRedPacket.getRedPacketId();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String sql2 = "insert into T_USER_RED_PACKET(red_packet_id, user_id, " + "amount, grab_time, note)"
                        + " values (" + userRedPacket.getRedPacketId() + ", " + userRedPacket.getUserId() + ", "
                        + userRedPacket.getAmount() + "," + "'" + df.format(userRedPacket.getGetTime()) + "'," + "'"
                        + userRedPacket.getNote() + "')";
                stmt.addBatch(sql1);
                stmt.addBatch(sql2);
            }
            // 执行批量
            count = stmt.executeBatch();
            // 提交事务
            conn.commit();
        } catch (SQLException e) {
            /********* 错误处理逻辑 ********/
            throw new RuntimeException("抢红包批量执行程序错误");
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // 返回插入抢红包数据记录
        return count.length / 2;
    }
}
