package com.kwan.springbootkwan.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwan.springbootkwan.entity.CsdnFollowFansInfo;
import com.kwan.springbootkwan.entity.csdn.DeleteFollowQuery;
import com.kwan.springbootkwan.entity.csdn.DeleteFollowResponse;
import com.kwan.springbootkwan.entity.csdn.FansResponse;
import com.kwan.springbootkwan.mapper.CsdnFollowFansInfoMapper;
import com.kwan.springbootkwan.service.CsdnFollowFansInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CsdnFollowFansInfoServiceImpl extends ServiceImpl<CsdnFollowFansInfoMapper, CsdnFollowFansInfo> implements CsdnFollowFansInfoService {

    @Value("${csdn.cookie}")
    private String csdnCookie;
    @Value("${csdn.self_user_name}")
    private String selfUserName;

    @Override
    public void saveFans() {
        //删除全部数据
        QueryWrapper<CsdnFollowFansInfo> wrapper = new QueryWrapper<>();
        this.remove(wrapper);
        Integer fanId = this.getFanId();
        while (Objects.nonNull(fanId)) {
            fanId = this.saveWithFanId(fanId);
        }
    }

    @Override
    public void saveFollow() {
        final List<CsdnFollowFansInfo> all = getAll();
        if (CollectionUtil.isNotEmpty(all) && all.size() >= 1500) {
            Integer fanId = this.getFollowId();
            while (Objects.nonNull(fanId)) {
                fanId = this.saveWithFollowId(fanId);
            }
        }
    }

    @Override
    public void deleteFollow() {
        final List<CsdnFollowFansInfo> onlyFollow;
        try {
            onlyFollow = getOnlyFollow();
            if (CollectionUtil.isNotEmpty(onlyFollow)) {
                for (CsdnFollowFansInfo csdnFollowFansInfo : onlyFollow) {
                    //取消关注
                    DeleteFollowQuery deleteFollowQuery = new DeleteFollowQuery();
                    deleteFollowQuery.setUsername(selfUserName);
                    deleteFollowQuery.setDetailSourceName("个人主页");
                    deleteFollowQuery.setFollow(csdnFollowFansInfo.getUserName());
                    deleteFollowQuery.setFromType("pc");
                    deleteFollowQuery.setSource("ME");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonCollectInfo = objectMapper.writeValueAsString(deleteFollowQuery);
                    HttpResponse response = HttpUtil.createPost("https://mp-action.csdn.net/interact/wrapper/pc/fans/v1/api/unFollow")
                            .header("Cookie", csdnCookie)
                            .header("Content-Type", "application/json")
                            .body(jsonCollectInfo)
                            .execute();
                    final String body = response.body();
                    DeleteFollowResponse collectResponse = objectMapper.readValue(body, DeleteFollowResponse.class);
                    final Integer code = collectResponse.getCode();
                    final String msg = collectResponse.getMsg();
                    if ("200".equals(code.toString())) {
                        //取消成功了直接删除
                        csdnFollowFansInfo.setIsDelete(1);
                        this.updateById(csdnFollowFansInfo);
                    } else {
                        log.info("取消关注失败,code={},msg={}", code, msg);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public List<CsdnFollowFansInfo> getAll() {
        QueryWrapper<CsdnFollowFansInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("is_delete", 0);
        return this.list(wrapper);
    }

    @Override
    public List<CsdnFollowFansInfo> getOnlyFollow() {
        QueryWrapper<CsdnFollowFansInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("is_delete", 0);
        wrapper.eq("relation_type", "已关注");
        return this.list(wrapper);
    }

    @Override
    public CsdnFollowFansInfo getByUserName(String userName) {
        QueryWrapper<CsdnFollowFansInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("is_delete", 0);
        wrapper.eq("user_name", userName);
        return this.getOne(wrapper);
    }

    private Integer getFanId() {
        Integer fanId = null;
        try {
            //get方法
            String url = "https://mp-action.csdn.net/interact/wrapper/pc/fans/v1/api/getFansOffsetList?pageSize=20&username=" + selfUserName;
            HttpResponse response = HttpUtil.createGet(url)
                    .header("Cookie", csdnCookie)
                    .execute();
            final String body = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            FansResponse articleInfo = objectMapper.readValue(body, FansResponse.class);
            final FansResponse.FansData data = articleInfo.getData();
            fanId = data.getFanId();
            final List<FansResponse.FansData.FansListData> list = data.getList();
            if (CollectionUtil.isNotEmpty(list)) {
                for (FansResponse.FansData.FansListData fansListData : list) {
                    final String username = fansListData.getUsername();
                    CsdnFollowFansInfo byUserName = getByUserName(username);
                    if (Objects.isNull(byUserName)) {
                        byUserName = new CsdnFollowFansInfo();
                        byUserName.setUserName(username);
                        byUserName.setNickName(fansListData.getNickname());
                        byUserName.setBlogUrl(fansListData.getBlogUrl());
                        byUserName.setRelationType("粉丝");
                        this.save(byUserName);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return fanId;
    }

    private Integer saveWithFanId(Integer fanId) {
        try {
            //get方法
            String url = "https://mp-action.csdn.net/interact/wrapper/pc/fans/v1/api/getFansOffsetList?pageSize=20&username=" + selfUserName + "&fanId=" + fanId;
            HttpResponse response = HttpUtil.createGet(url)
                    .header("Cookie", csdnCookie)
                    .execute();
            final String body = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            FansResponse articleInfo = objectMapper.readValue(body, FansResponse.class);
            final FansResponse.FansData data = articleInfo.getData();
            fanId = data.getFanId();
            final List<FansResponse.FansData.FansListData> list = data.getList();
            if (CollectionUtil.isNotEmpty(list)) {
                for (FansResponse.FansData.FansListData fansListData : list) {
                    final String username = fansListData.getUsername();
                    QueryWrapper<CsdnFollowFansInfo> wrapper = new QueryWrapper<>();
                    wrapper.eq("is_delete", 0);
                    wrapper.eq("user_name", username);
                    CsdnFollowFansInfo csdnFollowFansInfo = this.getOne(wrapper);
                    if (Objects.isNull(csdnFollowFansInfo)) {
                        csdnFollowFansInfo = new CsdnFollowFansInfo();
                        csdnFollowFansInfo.setUserName(username);
                        csdnFollowFansInfo.setNickName(fansListData.getNickname());
                        csdnFollowFansInfo.setBlogUrl(fansListData.getBlogUrl());
                        csdnFollowFansInfo.setRelationType("粉丝");
                        this.save(csdnFollowFansInfo);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return fanId;
    }


    private Integer getFollowId() {
        Integer fanId = null;
        try {
            //get方法
            String url = "https://mp-action.csdn.net/interact/wrapper/pc/fans/v1/api/getFollowOffsetList?pageSize=20&username=" + selfUserName;
            HttpResponse response = HttpUtil.createGet(url)
                    .header("Cookie", csdnCookie)
                    .execute();
            final String body = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            FansResponse articleInfo = objectMapper.readValue(body, FansResponse.class);
            final FansResponse.FansData data = articleInfo.getData();
            fanId = data.getFanId();
            final List<FansResponse.FansData.FansListData> list = data.getList();
            if (CollectionUtil.isNotEmpty(list)) {
                for (FansResponse.FansData.FansListData fansListData : list) {
                    final String username = fansListData.getUsername();
                    CsdnFollowFansInfo byUserName = getByUserName(username);
                    if (Objects.nonNull(byUserName)) {
                        byUserName.setRelationType("互关");
                        this.updateById(byUserName);
                    } else {
                        //取消关注
                        byUserName = new CsdnFollowFansInfo();
                        byUserName.setUserName(username);
                        byUserName.setNickName(fansListData.getNickname());
                        byUserName.setBlogUrl(fansListData.getBlogUrl());
                        byUserName.setRelationType("已关注");
                        this.save(byUserName);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return fanId;
    }

    private Integer saveWithFollowId(Integer fanId) {
        try {
            //get方法
            String url = "https://mp-action.csdn.net/interact/wrapper/pc/fans/v1/api/getFollowOffsetList?pageSize=20&username=" + selfUserName + "&fanId=" + fanId;
            HttpResponse response = HttpUtil.createGet(url)
                    .header("Cookie", csdnCookie)
                    .execute();
            final String body = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            FansResponse articleInfo = objectMapper.readValue(body, FansResponse.class);
            final FansResponse.FansData data = articleInfo.getData();
            fanId = data.getFanId();
            final List<FansResponse.FansData.FansListData> list = data.getList();
            if (CollectionUtil.isNotEmpty(list)) {
                for (FansResponse.FansData.FansListData fansListData : list) {
                    final String username = fansListData.getUsername();
                    QueryWrapper<CsdnFollowFansInfo> wrapper = new QueryWrapper<>();
                    wrapper.eq("is_delete", 0);
                    wrapper.eq("user_name", username);
                    CsdnFollowFansInfo csdnFollowFansInfo = this.getOne(wrapper);
                    if (Objects.nonNull(csdnFollowFansInfo)) {
                        csdnFollowFansInfo.setRelationType("互关");
                        this.updateById(csdnFollowFansInfo);
                    } else {
                        //取消关注
                        csdnFollowFansInfo = new CsdnFollowFansInfo();
                        csdnFollowFansInfo.setUserName(username);
                        csdnFollowFansInfo.setNickName(fansListData.getNickname());
                        csdnFollowFansInfo.setBlogUrl(fansListData.getBlogUrl());
                        csdnFollowFansInfo.setRelationType("已关注");
                        this.save(csdnFollowFansInfo);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return fanId;
    }
}

