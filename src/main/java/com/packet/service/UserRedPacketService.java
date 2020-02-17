package com.packet.service;

import com.packet.entity.RedPacket;
import com.packet.entity.UserRedPacket;
import com.packet.repository.RedPacketRepository;
import com.packet.repository.RedPacketRepositoryOL;
import com.packet.repository.UserPacketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.sql.Timestamp;


@Service
public class UserRedPacketService {
    @Autowired
    private UserPacketRepository userPacketRepository;
    @Autowired
    private RedPacketRepository redPacketRepository;
    @Autowired
    private RedPacketRepositoryOL redPacketRepositoryOL;
    @Autowired
    private RedPacketService redPacketService;
    @Autowired
    private RedisTemplate redisTemplate;

    private static final int FAILED = 0;

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int getPacket(Long redPacketId, Long userId) {
        RedPacket redPacket = redPacketRepository.getRedPacket(redPacketId);
        //判断红包还有剩余
        if (redPacket.getStock() > 0) {
            redPacketRepository.decrRedPacket(redPacketId);

            UserRedPacket userRedPacket = new UserRedPacket();
            userRedPacket.setRedPacketId(redPacketId);
            userRedPacket.setUserId(userId);
            userRedPacket.setAmount(redPacket.getUnitAmount());
            userRedPacket.setGetTime((new Timestamp(System.currentTimeMillis())));
            userRedPacket.setNote("抢到红包:" + redPacketId);

            int result = userPacketRepository.getPacket(userRedPacket);
            return result;
        }
        return FAILED;
    }

    /**
     * 乐观锁解决方案
     *
     * @param redPacketId
     * @param userId
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int getPacketForVersion(Long redPacketId, Long userId) {
        RedPacket redPacket = redPacketRepository.getRedPacket(redPacketId);
        if (redPacket.getStock() > 0) {
            int update = redPacketRepositoryOL.decrRedPacketForVersion(redPacketId, redPacket.getVersion());
            if (update == 0) {
                return FAILED;
            }

            UserRedPacket userRedPacket = new UserRedPacket();
            userRedPacket.setRedPacketId(redPacketId);
            userRedPacket.setUserId(userId);
            userRedPacket.setAmount(redPacket.getUnitAmount());
            userRedPacket.setGetTime((new Timestamp(System.currentTimeMillis())));
            userRedPacket.setNote("抢到红包:" + redPacketId);

            int result = userPacketRepository.getPacket(userRedPacket);
            return result;
        }
        return FAILED;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int getPacketForVersion2(Long redPacketId, Long userId) {
        long start = System.currentTimeMillis();
        while (true) {
            long end = System.currentTimeMillis();
            //短时间内重试
            if (end - start > 100) {
                return FAILED;
            }
            RedPacket redPacket = redPacketRepository.getRedPacket(redPacketId);
            if (redPacket.getStock() > 0) {
                int update = redPacketRepositoryOL.decrRedPacketForVersion(redPacketId, redPacket.getVersion());
                if (update == 0) {
                    continue;
                }

                UserRedPacket userRedPacket = new UserRedPacket();
                userRedPacket.setRedPacketId(redPacketId);
                userRedPacket.setUserId(userId);
                userRedPacket.setAmount(redPacket.getUnitAmount());
                userRedPacket.setGetTime((new Timestamp(System.currentTimeMillis())));
                userRedPacket.setNote("抢到红包:" + redPacketId);

                int result = userPacketRepository.getPacket(userRedPacket);
                return result;
            } else {
                return FAILED;
            }
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int getPacketForVersion3(Long redPacketId, Long userId) {
        //三次重试
        for (int i = 0; i < 3; i++) {
            RedPacket redPacket = redPacketRepository.getRedPacket(redPacketId);
            if (redPacket.getStock() > 0) {
                int update = redPacketRepositoryOL.decrRedPacketForVersion(redPacketId, redPacket.getVersion());
                if (update == 0) {
                    continue;
                }

                UserRedPacket userRedPacket = new UserRedPacket();
                userRedPacket.setRedPacketId(redPacketId);
                userRedPacket.setUserId(userId);
                userRedPacket.setAmount(redPacket.getUnitAmount());
                userRedPacket.setGetTime((new Timestamp(System.currentTimeMillis())));
                userRedPacket.setNote("抢到红包:" + redPacketId);

                int result = userPacketRepository.getPacket(userRedPacket);
                return result;
            } else {
                return FAILED;
            }
        }
        return FAILED;
    }


    // Lua脚本
    String script = "local listKey = 'red_packet_list_'..KEYS[1] \n"
            + "local redPacket = 'red_packet_'..KEYS[1] \n"
            + "local stock = tonumber(redis.call('hget', redPacket, 'stock')) \n"
            + "if stock <= 0 then return 0 end \n"
            + "stock = stock -1 \n"
            + "redis.call('hset', redPacket, 'stock', tostring(stock)) \n"
            + "redis.call('rpush', listKey, ARGV[1]) \n"
            + "if stock == 0 then return 2 end \n"
            + "return 1 \n";

    // 在缓存LUA脚本后，使用该变量保存Redis返回的32位的SHA1编码，使用它去执行缓存的LUA脚本[加入这句话]
    String sha1 = null;

    public Long getRedPacketByRedis(Long redPacketId, Long userId) {
        // 当前抢红包用户和日期信息
        String args = userId + "-" + System.currentTimeMillis();
        Long result = null;
        // 获取底层Redis操作对象
        Jedis jedis = (Jedis) redisTemplate.getConnectionFactory().getConnection().getNativeConnection();
        try {
            // 如果脚本没有加载过，那么进行加载，这样就会返回一个sha1编码
            if (sha1 == null) {
                sha1 = jedis.scriptLoad(script);
            }
            // 执行脚本，返回结果
            Object res = jedis.evalsha(sha1, 1, redPacketId + "", args);
            result = (Long) res;
            // 返回2时为最后一个红包，此时将抢红包信息通过异步保存到数据库中
            if (result == 2) {
                // 获取单个小红包金额
                String unitAmountStr = jedis.hget("red_packet_" + redPacketId, "unit_amount");
                // 触发保存数据库操作
                Double unitAmount = Double.parseDouble(unitAmountStr);
                System.err.println("thread_name = " + Thread.currentThread().getName());
                redPacketService.saveUserRedPacketByRedis(redPacketId, unitAmount);
            }
        } finally {
            // 确保jedis顺利关闭
            if (jedis != null && jedis.isConnected()) {
                jedis.close();
            }
        }
        return result;
    }
}
