package com.packet.repository;

import com.packet.entity.RedPacket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RedPacketRepositoryOL {
    /**
     * 查询红包信息
     * @param id
     * @return
     */
    @Select("select * from t_red_packet where id = #{id}")
    RedPacket getRedPacket(@Param("id") Long id);

    /**
     * 红包减扣
     * @param id
     * @return
     */
    @Update("update t_red_packet set stock = stock -1,version = version + 1 where id = #{id} and version = #{version}")
    int decrRedPacketForVersion(@Param("id") Long id,@Param("version") Integer version);
}
