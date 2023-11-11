package com.kwan.springbootkwan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.kwan.springbootkwan.entity.CsdnHistorySession;
import com.kwan.springbootkwan.entity.csdn.MessageResponse;

import java.util.List;

/**
 * 私信相关
 *
 * @author : qinyingjie
 * @version : 2.2.0
 * @date : 2023/11/9 23:58
 */
public interface CsdnMessageService extends IService<CsdnHistorySession> {
    /**
     * 分页查询私信列表
     *
     * @return
     */
    List<MessageResponse.MessageData.Sessions> acquireMessage();


    /**
     * 处理私信列表
     *
     * @param acquireMessage
     */
    void dealMessage(List<MessageResponse.MessageData.Sessions> acquireMessage);

    /**
     * 回复私信
     *
     * @param toUsername
     * @param messageType
     * @param messageBody
     * @param fromClientType
     * @param fromDeviceId
     * @param appId
     */
    void replyMessage(String toUsername, Integer messageType, String messageBody, String fromClientType, String fromDeviceId, String appId);

    /**
     * 私信已读
     *
     * @param toUsername
     */
    void messageRead(String toUsername);

    /**
     * 是否回复过私信
     */
    Boolean haveRepliedMessage(String fromUsername, String blogUrl);

    /**
     * 根据username回复私信
     *
     * @param username
     */
    void dealMessageByUserName(String username);

    /**
     * 获取私信人信息
     *
     * @param username
     * @return
     */
    CsdnHistorySession getCsdnHistorySession(String username);
}