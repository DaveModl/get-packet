package com.packet.entity;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
@Data
public class UserRedPacket implements Serializable {
    private static final long serialVersionUID = -7236778114195083598L;
    private Long id;
    private Long redPacketId;
    private Long userId;
    private Double amount;
    private Timestamp getTime;
    private String note;
}
