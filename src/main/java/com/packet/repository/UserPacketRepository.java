package com.packet.repository;

import com.packet.entity.UserRedPacket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;


@Mapper
@Repository
public interface UserPacketRepository {
    /**
     * 记录抢到的红包
     * @param userRedPacket
     * @return
     */
    @Insert("insert into  t_user_red_packet(red_packet_id,user_id,amount,get_time,note) " +
            "values (#{redPacketId},#{userId},#{amount},#{getTime,jdbcType=TIMESTAMP},#{note})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    int getPacket( UserRedPacket userRedPacket);
}
