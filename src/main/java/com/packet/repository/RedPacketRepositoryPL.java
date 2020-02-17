package com.packet.repository;

import com.packet.entity.RedPacket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RedPacketRepositoryPL {
    /**
     * 查询红包信息
     * 悲观锁实现，数据更新加锁，一旦一个线程持有record,其他线程就不能更新了
     * @param id
     * @return
     */
    @Select("select * from t_red_packet where id = #{id} for update")
    RedPacket getRedPacket(@Param("id") Long id);

    /**
     * 红包减扣
     * @param id
     * @return
     */
    @Update("update t_red_packet set stock = stock -1 where id = #{id}")
    int decrRedPacket(@Param("id") Long id);
}
