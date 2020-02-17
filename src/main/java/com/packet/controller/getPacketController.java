package com.packet.controller;

import com.packet.service.UserRedPacketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/packet")
public class getPacketController {
    @Autowired
    private UserRedPacketService userRedPacketService;
    @RequestMapping("/getpacket")
    @ResponseBody
    public Map<String,Object> getPacket(Long redPacketId,Long userId){
        int res = userRedPacketService.getPacket(redPacketId,userId);
        Map<String,Object> resMap = new HashMap<>();
        boolean flag = res > 0;
        resMap.put("success",flag);
        resMap.put("msg",flag ? "get success" : "get fail");
        return resMap;
    }

    @RequestMapping("/getpacket/forver")
    @ResponseBody
    public Map<String,Object> getPacketForVersion(Long redPacketId,Long userId){
        int res = userRedPacketService.getPacketForVersion(redPacketId,userId);
        Map<String,Object> resMap = new HashMap<>();
        boolean flag = res > 0;
        resMap.put("success",flag);
        resMap.put("msg",flag ? "get success" : "get fail");
        return resMap;
    }

    @RequestMapping("/getpacket/redis")
    @ResponseBody
    public Map<String, Object> grapRedPacketByRedis(Long redPacketId, Long userId) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Long result = userRedPacketService.getRedPacketByRedis(redPacketId, userId);
        boolean flag = result > 0;
        resultMap.put("result", flag);
        resultMap.put("message", flag ? "抢红包成功": "抢红包失败");
        return resultMap;
    }
}
