package com.ruoyi.project.system.gbdata.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.GbDataConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.gbdata.GetAppKeyUtils;
import com.ruoyi.common.utils.http.HttpClientUtil;
import com.ruoyi.project.system.gbdata.domain.*;
import com.ruoyi.project.system.gbdata.mapper.impl.GbDataViewMapperImpl;
import com.ruoyi.project.system.gbdata.service.IGbDataService;
import com.ruoyi.project.system.gbdata.service.ImplementionOrgCodeService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by issuser on 2019/11/14.
 */
@Service
public class GbDataServiceImpl implements IGbDataService {
    private static final Logger logger = LoggerFactory.getLogger(GbDataServiceImpl.class);
    @Autowired
    private RefreshSignKeyServiceImpl refreshSignKeyServiceImpl;

    @Autowired
    private GbDataViewMapperImpl gbDataViewMapper;

    @Autowired
    private ImplementionOrgCodeService implementionOrgCodeService;

    @Override
    public String getGbDataByHttp(String tableId, String testMod, String date) {
        //1. 获取参数拼接http请求头
        String secretKey = refreshSignKeyServiceImpl.getSignSecret();
        String gjgxjhpt_rtime = refreshSignKeyServiceImpl.getGjgxjhptRtime();
        Map<String, String> headMap = GetAppKeyUtils.getHeaderMap(GbDataConstants.GJGXJHPT_SID_VAL, GbDataConstants.GJGXJHPT_RID_VAL, secretKey, gjgxjhpt_rtime);
        //2. 拼接http请求体
        Map<String, String> bodyMap = Maps.newHashMap();
        List<String> pullDateList = null;

        if(isTestMode(testMod)){
            pullDateList = Lists.newArrayList();
            pullDateList.add(date);
        }else{
            pullDateList = getPullDateList(tableId);
        }

        int errorCount = 0;
        int correctCount = 0;
        String lastErrorMsg = "";
        String result = "";
        //bodyMap.putAll(headMap);
        for(String dateStr : pullDateList){
            bodyMap.put(GbDataConstants.CD_SOURCE, GbDataConstants.CD_SOURCE_VAL);
            bodyMap.put(GbDataConstants.SUBMITTED_DATE, dateStr);
            bodyMap.put(GbDataConstants.TABLE_ID, tableId);
            //3. 发送http请求
            result = HttpClientUtil.doGetNew(GbDataConstants.HTTP_PROXY_URL, bodyMap, headMap);

            //非test mode时才入库
            if(!isTestMode(testMod)){
                //4. 正确的响应结果转换为实体类
                if(result.contains("errCode")){
                    JSONObject errJson = JSONObject.parseObject(result);
                    lastErrorMsg = errJson.getString("errCode") + errJson.getString(" errMsg");
                    logger.error("Connection to gb jgxw error: " + lastErrorMsg);
                    errorCount++;
                }else if(result.contains("没有数据或者时间格式错误")){
                    logger.info(result);
                }else{
                    logger.info(result);
                    addJgxwData(result, tableId, dateStr);
                    correctCount++;
                }
            }
        }

        String response;
        if(isTestMode(testMod)){
            response = result;
        }else{
            StringBuilder rsBuilder = new StringBuilder();
            rsBuilder.append("correct response=").append(correctCount).append(" , ")
                    .append("error response=").append(errorCount);

            if(StringUtils.isNotEmpty(lastErrorMsg)){
                rsBuilder.append(";Last error message=").append(lastErrorMsg);
            }

            response = rsBuilder.toString();
        }

        return response;
    }

    private boolean isTestMode(String testMode){
        return testMode.equals("1");
    }

    private List<String> getPullDateList(String tableId){
        String startDate;
        String lastPullDate = refreshSignKeyServiceImpl.getGbDataLastPullDate(tableId);
        List<String> pullDateList = Lists.newArrayList();
        //没有拉取过数据
        if(lastPullDate.equals("0")){
            startDate = refreshSignKeyServiceImpl.getGbDataStartDate();
        }else{
            startDate = lastPullDate;
        }

        String nowDate = DateUtils.getDate();
        if(DateUtils.before(startDate, nowDate)){
            while(DateUtils.before(startDate, nowDate)){
                startDate = DateUtils.addOneDay(startDate);
                pullDateList.add(startDate);
            }
        }
        return pullDateList;
    }

    @Override
    public String manualAddData(String json, String tableId){
        if(StringUtils.isEmpty(json) || StringUtils.isEmpty(tableId)){
            return "Request params error!";
        }

        String response;
        try {
            String nowDate = DateUtils.getDate();
            addJgxwData(json, tableId, nowDate);
            response = "add data successful";
        } catch (Exception e) {
            e.printStackTrace();
            String eMsg = StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : e.getCause().getMessage();
            response = "add data failure: " + eMsg;
        }
        return response;
    }

    private void addJgxwData(String result, String tableId, String dateStr) {
        //保存数据到数据库
        //1. tableId = 1, JgJgxwCheckAction
        //2. tableId = 2, JgJgxwForceAction
        //3. tableId = 3, JgJgxwOtherAction
        //4. tableId = 4, JgJgxwPunishAction
        try{
            JSONArray jsonArray = JSONArray.parseArray(result);
            Iterator<Object> jsonObjectIter = jsonArray.iterator();
            while(jsonObjectIter.hasNext()){
                JSONObject jsonObject = (JSONObject) jsonObjectIter.next();
                doInsertOfDifferentTable(jsonObject, tableId);
            }
            //更新data_dictionary表和内存数据
            refreshSignKeyServiceImpl.updateDateLastPullDate(tableId, dateStr);
            logger.info("Get Gb data success.");
        }catch (Exception e){
            String exStr = StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : e.getCause().getMessage();
            logger.error(exStr);
            logger.error("Result contains error: " + result);

            ErrorRecord errorRecord = new ErrorRecord();
            errorRecord.setTableId(tableId);
            errorRecord.setData(result);
            try {
                gbDataViewMapper.insertErrorRecord(errorRecord);
            } catch (Exception e1) {
                String exMsg = StringUtils.isNotEmpty(e1.getMessage()) ? e1.getMessage() : e1.getCause().getMessage();
                logger.error(exMsg);
            }
        }
    }

    private void doInsertOfDifferentTable(JSONObject jsonObject, String tableId) throws Exception{
        if(tableId.equals("1")){
            JgJgxwCheckAction jgxwCheckAction = JSON.toJavaObject(jsonObject, JgJgxwCheckAction.class);
            jgxwCheckAction.setCdSource(GbDataConstants.CD_SOURCE_JG);
            jgxwCheckAction.setCdTime(new Date());
            jgxwCheckAction = implementionOrgCodeService.buildCdHbSource(jgxwCheckAction);
            gbDataViewMapper.insertJgJgxwCheckAction(jgxwCheckAction);
        }else if(tableId.equals("2")){
            JgJgxwForceAction jgJgxwForceAction = JSON.toJavaObject(jsonObject, JgJgxwForceAction.class);
            jgJgxwForceAction.setCdSource(GbDataConstants.CD_SOURCE_JG);
            jgJgxwForceAction.setCdTime(new Date());
            jgJgxwForceAction = implementionOrgCodeService.buildCdHbSource(jgJgxwForceAction);
            gbDataViewMapper.insertJgxwForceAction(jgJgxwForceAction);
        }else if(tableId.equals("3")){
            JgJgxwOtherAction jgJgxwOtherAction = JSON.toJavaObject(jsonObject, JgJgxwOtherAction.class);
            jgJgxwOtherAction.setCdSource(GbDataConstants.CD_SOURCE_JG);
            jgJgxwOtherAction.setCdTime(new Date());
            jgJgxwOtherAction = implementionOrgCodeService.buildCdHbSource(jgJgxwOtherAction);
            gbDataViewMapper.insertJgxwOtherAction(jgJgxwOtherAction);
        }else if(tableId.equals("4")){
            JgJgxwPunishAction jgJgxwPunishAction = JSON.toJavaObject(jsonObject, JgJgxwPunishAction.class);
            jgJgxwPunishAction.setCdSource(GbDataConstants.CD_SOURCE_JG);
            jgJgxwPunishAction.setCdTime(new Date());
            jgJgxwPunishAction = implementionOrgCodeService.buildCdHbSource(jgJgxwPunishAction);
            gbDataViewMapper.insertJgxwPunishAction(jgJgxwPunishAction);
        }
    }

    @Override
    public void getGbdataByWs(String tableId) {

    }
}
