package com.ruoyi.project.system.data.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ruoyi.project.system.data.mapper.DataViewMapper;
import com.ruoyi.project.system.opt.domain.TableDataOpt;
import com.ruoyi.project.system.opt.mapper.TableDataOptMapper;
import com.ruoyi.project.system.recode.domain.AccountRecode;
import com.ruoyi.project.system.recode.mapper.AccountRecodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.beans.Transient;
import java.util.*;

/**
 * 用户 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class DataViewServiceImpl implements IDataViewService {

    private static final Logger log = LoggerFactory.getLogger(DataViewServiceImpl.class);

    @Resource
    private DataViewMapper dataViewMapper;

    @Resource
    private TableDataOptMapper tableDataOptMapper;

    @Resource
    private AccountRecodeMapper accountRecodeMapper;

    @Override
    public List<JSONObject> accountQZ() {
        List<JSONObject> tableInfos = dataViewMapper.getDsTableInfos();
        for (JSONObject tableInfo : tableInfos){
            Long count = dataViewMapper.getQZTableData(tableInfo);
            tableInfo.put("count",count);
        }
        deleteAndInsert(tableInfos,"地市前置数据统计",1L);
        return tableInfos;
    }

    @Override
    public List<JSONObject> accountSz() {
        List<JSONObject> tableInfos = dataViewMapper.getSzTableInfos();
        for (JSONObject tableInfo : tableInfos){
            Long count = dataViewMapper.getQZTableData(tableInfo);
            tableInfo.put("count",count);
        }
        deleteAndInsert(tableInfos,"省直前置数据统计",5L);
        return tableInfos;
    }

    @Override
    public List<JSONObject> getQzCity() {
        List<JSONObject> areas = getSourceName();
        List<JSONObject> ds = new ArrayList<>();
        for (JSONObject area : areas){
            if (area.getString("database_name").contains("ds_")){
                ds.add(area);
            }
        }
        return ds;
    }

    @Override
    public JSONArray accountDataColumns(){
        List<JSONObject> areas = getSourceName();
        JSONArray column0 = new JSONArray();
        JSONArray column1 = new JSONArray();
        JSONArray columns = new JSONArray();
        Map<String,Object> title = new LinkedHashMap<>();
        title.put("field","tableComment");
        title.put("title","表注释");
        title.put("rowspan",2);
        Map<String,Object> title2 = new LinkedHashMap<>();
        title2.put("field","tableName");
        title2.put("title","表名");
        title2.put("rowspan",2);
        column0.add(title);
        column0.add(title2);
        int index = 0;
        for (JSONObject opt : areas){
            column0.add(getTitle0(opt.getString("ORG_NAME")));
            column1.addAll(getTitle1(index));
            index++;
        }
        columns.add(column0);
        columns.add(column1);
        return columns;
    }

    @Override
    public JSONArray accountQzColumnsRequired() {
        JSONArray array = new JSONArray();
        List<JSONObject> list = dataViewMapper.accountQzColumnsRequired();
        JSONObject jsonObject = new JSONObject();
        String str = "，CD_ID，CD_TIME，CD_SOURCE，CD_OPERATION，DATA_UP_TIME，DATA_UP_STATUS，DATA_UP_UUID";
        String[] required = {"CD_ID", "CD_TIME", "CD_SOURCE", "CD_OPERATION", "DATA_UP_TIME", "DATA_UP_STATUS", "DATA_UP_UUID"};
        for (JSONObject column : list){
            if(!column.getString("TABLE_NAME").equalsIgnoreCase(jsonObject.getString("TABLE_NAME")) && jsonObject.get("TABLE_NAME") != null){
                if (str.indexOf("，") == 0){
                    str = str.replaceFirst("，","");
                }
                jsonObject.put("lack_field",str);
                array.add(jsonObject);
                jsonObject = new JSONObject();
                str = "，CD_ID，CD_TIME，CD_SOURCE，CD_OPERATION，DATA_UP_TIME，DATA_UP_STATUS，DATA_UP_UUID";
            }
            for (int i=0;i<required.length;i++){
                if (column.getString("COLUMN_NAME").equalsIgnoreCase(required[i])){
                    str = str.replace("，"+required[i],"");
                    break;
                }
            }
            jsonObject.put("TABLE_SCHEMA",column.getString("TABLE_SCHEMA"));
            jsonObject.put("TABLE_NAME",column.getString("TABLE_NAME"));
            jsonObject.put("ORG_NAME",column.getString("ORG_NAME"));
        }
        return array;
    }

    @Override
    public JSONArray accountData(){
        JSONArray array = new JSONArray();
        List<JSONObject> areas = getSourceName();
        List<JSONObject> tableInfos = dataViewMapper.getJCTableInfos();
        //查询前置库表
        List<JSONObject> qzTableInfos = dataViewMapper.getDsTableInfos();
        for (JSONObject tableInfo : tableInfos){
            if(!tableInfo.getString("table_name").contains("jg_jgsx_")) {//不统计监管事项
                //List<JSONObject> datas  = dataViewMapper.getZLData(tableInfo);
                List<JSONObject> datas = dataViewMapper.getBZData(tableInfo);
                List<JSONObject> dataZQ = dataViewMapper.getZQData(tableInfo);
                for (JSONObject jsonObject : datas) {
                    jsonObject.put("zqCount", 0);
                    jsonObject.put("cwCount", 0);
                    for (JSONObject object : dataZQ) {
                        if (jsonObject.getString("CD_HB_SOURCE").equals(object.getString("CD_HB_SOURCE"))) {
                            jsonObject.put("cwCount", jsonObject.getLong("zlCount") - object.getLong("zqCount"));
                            jsonObject.put("zqCount", object.getLong("zqCount"));
                        }
                    }
                }
                Map<String, Object> tableData = new LinkedHashMap<>();
                int index = 0;
                for (JSONObject area : areas) {
                    Long qzCount = 0L;
                    Long zlCount = 0L;
                    Long zqCount = 0L;
                    Long cwCount = 0L;
                    String name = area.getString("ORG_NAME");
                    String str = area.getString("CD_HB_SOURCE");
                    for (JSONObject data : datas) {
                        //如果存在数据,覆盖数值为0的记录
                        if (str.equals(data.getString("CD_HB_SOURCE"))) {
                            zlCount = data.getLong("zlCount");
                            zqCount = data.getLong("zqCount");
                            cwCount = data.getLong("cwCount");
                        }
                    }
                    for (JSONObject qz : qzTableInfos) {
                        if (qz.getString("table_schema").equals(area.getString("database_name"))
                                && qz.getString("table_name").equals(tableInfo.getString("table_name"))) {
                            qzCount = dataViewMapper.getQZTableData(qz);
                            break;
                        }
                    }
                    tableData.put("name", name);
                    tableData.put(index + "_qzCount", qzCount);
                    tableData.put(index + "_zlCount", zlCount);
                    tableData.put(index + "_zqCount", zqCount);
                    tableData.put(index + "_cwCount", cwCount);
                    index++;
                }
                tableData.put("tableComment", tableInfo.getString("table_comment"));
                tableData.put("tableName", tableInfo.getString("table_name"));
                array.add(tableData);
            }
        }
        deleteAndInsert(array,"数据治理详细统计",2L);
        return array;
    }

    private List<JSONObject> getSourceName(){
        return dataViewMapper.getSourceName();
    }

    @Override
    public JSONArray getDsTableGroupColumns(){
        JSONArray column0 = new JSONArray();
        JSONArray column1 = new JSONArray();
        JSONArray columns = new JSONArray();
        Map<String,Object> title = new LinkedHashMap<>();
        title.put("field","0_city");
        title.put("title","名称");
        title.put("rowspan",2);
        title.put("align","center");
        column0.add(title);
        List<TableDataOpt> list = tableDataOptMapper.selectTableDataOptList(new TableDataOpt());
        int index = 0;
        for (TableDataOpt opt : list){
            column0.add(getTitle0(opt.getOptName()));
            column1.addAll(getTitle1(index));
            index++;
        }
        columns.add(column0);
        columns.add(column1);
        return columns;
    }


    @Override
    public JSONArray getJgswSzTableGroupColumns(){
        JSONArray column0 = new JSONArray();

        JSONArray columns = new JSONArray();

/*        List<TableDataOpt> list = tableDataOptMapper.selectTableDataOptList(new TableDataOpt());
        int index = 0;
        for (TableDataOpt opt : list){
            column0.add(getTitle0(opt.getOptName()));
            column1.addAll(getTitle1(index));
            index++;
        }*/
        column0.addAll(getTitle2());
        columns.add(column0);

        return columns;
    }

    @Override
    public JSONArray getJgswDsTableGroupColumns(){
        JSONArray column0 = new JSONArray();

        JSONArray columns = new JSONArray();

/*        List<TableDataOpt> list = tableDataOptMapper.selectTableDataOptList(new TableDataOpt());
        int index = 0;
        for (TableDataOpt opt : list){
            column0.add(getTitle0(opt.getOptName()));
            column1.addAll(getTitle1(index));
            index++;
        }*/
        column0.addAll(getTitle3());
        columns.add(column0);

        return columns;
    }

    @Override
    @Transient
    public List<Map<String, Object>> getDsTableGroupData(){
        //查询前置库表
        List<JSONObject> qzTableInfos = dataViewMapper.getDsTableInfos();
        List<Map<String, Object>> array = dataProcessing(qzTableInfos,"ds_");
        deleteAndInsert(array,"地市合表统计",0L);
        return array;
    }

    @Override
    @Transient
    public List<Map<String, Object>> getJgTableGroupData(){
        //查询前置库表
        List<JSONObject> qzTableInfos = dataViewMapper.getDsTableInfos();
        List<Map<String, Object>> array = dataProcessing(qzTableInfos,"ds_");
        deleteAndInsert(array,"地市合表统计",0L);
        return array;
    }

    @Override
    @Transient
    public List<Map<String, Object>> getSzTableGroupData(){
        //查询前置库表
        List<JSONObject> qzTableInfos = dataViewMapper.getSzTableInfos();
        List<Map<String, Object>> array = dataProcessing(qzTableInfos,"jg_");
        deleteAndInsert(array,"省直合表统计",3L);
        return array;
    }


    @Override
    @Transient
    public List<JSONObject> getSzJgsxZyfgl(){
        //查询查基础库 省直
        List<JSONObject> qzTableInfos = dataViewMapper.getSzJgsxZyfgl();
       // List<Map<String, Object>> array = dataProcessing(qzTableInfos,"jg_");
        deleteAndInsert(qzTableInfos,"省直覆盖率统计",6L);
        return qzTableInfos;
    }

    @Override
    @Transient
    public List<JSONObject> getDsJgsxZyfgl(){
        //查询基础库 地市
        List<JSONObject> qzTableInfos = dataViewMapper.getDsJgsxZyfgl();
      //  List<Map<String, Object>> array = dataProcessing(qzTableInfos,"jg_");
        deleteAndInsert(qzTableInfos,"地市覆盖率表统计",7L);
        return qzTableInfos;
    }

    @Override
    @Transient
    public List<JSONObject> getSzJgsxZyfglZq(){
        //查询查基础库 省直
        List<JSONObject> qzTableInfos = dataViewMapper.getSzJgsxZyfglZq();
        // List<Map<String, Object>> array = dataProcessing(qzTableInfos,"jg_");
        deleteAndInsert(qzTableInfos,"省直覆盖率统计",8L);
        return qzTableInfos;
    }

    @Override
    @Transient
    public List<JSONObject> getDsJgsxZyfglZq(){
        //查询基础库 地市
        List<JSONObject> qzTableInfos = dataViewMapper.getDsJgsxZyfglZq();
        //  List<Map<String, Object>> array = dataProcessing(qzTableInfos,"jg_");
        deleteAndInsert(qzTableInfos,"地市覆盖率表统计",9L);
        return qzTableInfos;
    }

    @Override
    public List<JSONObject> getQzColumnsError() {
        List<JSONObject> qzErrorColumns = dataViewMapper.getQzColumnsError();
        return qzErrorColumns;
    }

    @Override
    public List<JSONObject> getDsGjsxByCity(String table_schema) {
        List<JSONObject> tableInfos = dataViewMapper.getDsGjsxTableInfos(table_schema);
        List<JSONObject> list = new ArrayList<>();
        if (tableInfos != null && tableInfos.size() > 0){
            list = dataViewMapper.getDsGjsxByCity(tableInfos);
        }
        return list;
    }

 /*   @Override
    public List<JSONObject> getZlErrorData(){
        List<JSONObject> data = new ArrayList<>();
        List<JSONObject> tableInfos = dataViewMapper.getJCTableInfos();
        List<JSONObject> areas = getSourceName();
        for (JSONObject tableInfo : tableInfos){
            if(!tableInfo.getString("table_name").contains("jg_jgsx_")){//不统计监管事项
                List<JSONObject> list = dataViewMapper.getZlErrorData(tableInfo);
                for (JSONObject area : areas){
                    for (JSONObject json : list){
                        if (area.getString("CD_HB_SOURCE").equalsIgnoreCase(json.getString("CD_HB_SOURCE"))){
                            json.put("org_name",area.getString("ORG_NAME"));
                        }
                    }
                }
                data.addAll(list);
            }
        }
        deleteAndInsert(data,"错误数据统计",4L);
        return data;
    }*/

    @Override
    public List<JSONObject> getZlErrorData(){
        List<JSONObject> data = new ArrayList<>();
        List<JSONObject> tableInfos = dataViewMapper.getHJTableInfos();
        List<JSONObject> areas = getSourceName();
        List<JSONObject> existTableList = dataViewMapper.getExistTablesInErrorDB();

        Map<String, String> falseFdTableMap = Maps.newHashMap();
        Map<String, String> falseFiTableMap = Maps.newHashMap();

        for (JSONObject tableInfo : tableInfos){
            String tableName = tableInfo.getString("table_name");
            if(!tableName.contains("jg_jgsx_")
                    && !tableName.equalsIgnoreCase("management_org")
                    && !tableName.equalsIgnoreCase("user_info")
                    && !tableName.startsWith("table_")
                    && !tableName.equalsIgnoreCase("jg_tsjb_accessory")
                    && !tableName.equalsIgnoreCase("jg_zsk_risk")
                    && !tableName.equalsIgnoreCase("jg_zsk_rule")){//不统计监管事项
                getExistTablesInErrorDB(tableName, "_fd", existTableList, falseFdTableMap);
                getExistTablesInErrorDB(tableName, "_fi", existTableList, falseFiTableMap);
            }
        }
        doZlErrorDataQuery(data, areas, falseFdTableMap, falseFiTableMap);
        deleteAndInsert(data,"错误数据统计",4L);
        return data;
    }


    private void doZlErrorDataQuery(List<JSONObject> data, List<JSONObject> areas, Map<String, String> falseFdTableMap, Map<String, String> falseFiTableMap) {
        for(String tableName : falseFdTableMap.keySet()){
            String fdTableName = falseFdTableMap.get(tableName);
            String fiTableName = falseFiTableMap.get(tableName);

            List<JSONObject> list = dataViewMapper.getZlErrorData(tableName, fdTableName, fiTableName);
            for (JSONObject area : areas){
                for (JSONObject json : list){
                    if (area.getString("CD_HB_SOURCE").equalsIgnoreCase(json.getString("CD_HB_SOURCE"))){
                        json.put("org_name",area.getString("ORG_NAME"));
                    }
                }
            }
            data.addAll(list);
        }
    }

    private void getExistTablesInErrorDB(String tableName, String tag, List<JSONObject> existTableList, Map<String, String> tableNameMap){
        for(JSONObject table : existTableList){
            String existTableName = table.getString("table_name");
            //1. 判断错误库已有表中是否有汇聚库源表
            if(existTableName.startsWith(tableName) && existTableName.contains(tag)){
                //2. 如果包含，放到map中
                if(tableNameMap.get(tableName) == null){
                    tableNameMap.put(tableName, existTableName);
                }else{
                    //3. 如果存在map中的表名长度小于汇聚库同名的表，则使用长度更长的表名
                    //e.g. tsjb_report_info_fd; tsjb_report_info_fd_2;
                    String falseTableName = tableNameMap.get(tableName);
                    if(falseTableName.length() < existTableName.length()){
                        tableNameMap.put(tableName, existTableName);
                    }
                }
            }
        }
    }



    @Override
    public List<JSONObject> compareQzAndHj(){
        //查询前置库表
        List<JSONObject> dsTableInfos = dataViewMapper.getDsTableInfos();
        //查询前置库表
        List<JSONObject> szTableInfos = dataViewMapper.getSzTableInfos();
        dsTableInfos.addAll(szTableInfos);
        for (JSONObject jsonObject : dsTableInfos){
            long qz = dataViewMapper.getQZTableData(jsonObject);
            if (jsonObject.getString("table_name").equalsIgnoreCase("jg_ztk_jgxn_ssjygk_spot_chec")){
                jsonObject.put("table_name","jg_ztk_jgxn_ssjygk_spot_check");
            }
            long hj = dataViewMapper.getHjTableData(jsonObject);
            if(jsonObject.getString("table_schema").equals("jg_gsj") || jsonObject.getString("table_schema").equals("jg_zjj")){
                System.out.println("ceshi");
            }
            if (hj != qz){
                jsonObject.put("msg","数据不一致");
            }else {
                jsonObject.put("msg","");
            }
            jsonObject.put("qz",qz);
            jsonObject.put("hj",hj);
        }
        return dsTableInfos;
    }

    /**
     * 合表统计数据分析处理方法
     * @param tableInfos
     * @param type
     * @return
     */
    private List<Map<String, Object>> dataProcessing(List<JSONObject> tableInfos,String type){
        List<Map<String, Object>> array = new ArrayList<>();
        List<TableDataOpt> list = tableDataOptMapper.selectTableDataOptList(new TableDataOpt());
        //获取机构信息
        List<JSONObject> areas = getSourceName();
        for(JSONObject area : areas){
            //获取地市信息
            if (area.getString("database_name").contains(type)) {
                Map<String, Object> map = new LinkedHashMap<>();
                int index = 0;
                for (TableDataOpt opt : list) {
                    List<JSONObject> optInfos = JSONArray.parseArray(opt.getOptInfo(), JSONObject.class);
                    //存储前置库已存在的表
                    List<JSONObject> existTable = new ArrayList<>();
                    for (JSONObject optInfo : optInfos) {
                        for (JSONObject qzTable : tableInfos) {
                            if (qzTable.getString("table_schema").equals(area.getString("database_name"))
                                    && qzTable.getString("table_name").equals(optInfo.getString("tableName"))) {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("databaseName", area.getString("database_name"));
                                jsonObject.put("tableName", optInfo.getString("tableName"));
                                jsonObject.put("cdHbSource", area.getString("CD_HB_SOURCE"));
                                existTable.add(jsonObject);
                            }
                        }
                    }
                    map.put("0_city", area.getString("ORG_NAME"));
                    System.out.println("**********************" + index);
                    if (existTable.size() > 0) {
                        long num = dataViewMapper.getDataByCity(existTable);
                        JSONObject hjData = dataViewMapper.getDataByHJ(existTable);
                        JSONObject jcData = dataViewMapper.getDataByJC(existTable);
                        map.put(index+"_qzCount", num);
                        map.put(index+"_zlCount", hjData.getLong("zlCount"));
                        map.put(index+"_zqCount", jcData.getLong("zqCount"));
                        map.put(index+"_cwCount", hjData.getLong("zlCount")-jcData.getLong("zqCount"));
                    } else {
                        map.put(index+"_qzCount", 0);
                        map.put(index+"_zlCount", 0);
                        map.put(index+"_zqCount", 0);
                        map.put(index+"_cwCount", 0);
                    }
                    index++;
                }
                array.add(map);

            }
        }
        return array;
    }

    @Override
    public List<JSONObject> getExistingItem(){
        List<JSONObject> list = dataViewMapper.getExistingItem();
        return list;
    }

    @Override
    public List<JSONObject> getNotExistingItem(){
        List<JSONObject> list = dataViewMapper.getNotExistingItem();
        return list;
    }

    private void deleteAndInsert(Object array,String name,Long type){
        //存储数据到表
        AccountRecode recode = new AccountRecode();
        recode.setType(type);
        recode.setCreateTime(new Date());
        List<AccountRecode> recodes = accountRecodeMapper.selectAccountRecodeList(recode);
        if (recodes != null && recodes.size() > 0){
            String[] ids = new String[recodes.size()];
            for (int i=0;i<recodes.size();i++){
                ids[i] = recodes.get(i).getId()+"";
            }
            accountRecodeMapper.deleteAccountRecodeByIds(ids);
        }
        recode.setName(name);
        recode.setData(JSONObject.toJSONString(array));
        accountRecodeMapper.insertAccountRecode(recode);
    }

    private Map<String,Object> getTitle0(String name){
        Map<String,Object> title = new LinkedHashMap<>();
        title.put("title",name);
        title.put("colspan",4);
        title.put("align","center");
        return title;
    }
    private List<Map<String,Object>> getTitle1(int index){
        List<Map<String,Object>> list = new ArrayList<>();
        Map<String,Object> title0 = new LinkedHashMap<>();
        title0.put("field",index+"_qzCount");
        title0.put("title","前置数据量");
        title0.put("align","center");
        title0.put("width",150);
        list.add(title0);
        Map<String,Object> title1 = new LinkedHashMap<>();
        title1.put("field",index+"_zlCount");
        title1.put("title","标准数据量");
        title1.put("align","center");
        title1.put("width",150);
        list.add(title1);
        Map<String,Object> title2 = new LinkedHashMap<>();
        title2.put("field",index+"_zqCount");
        title2.put("title","正确量");
        title2.put("align","center");
        title2.put("width",150);
        list.add(title2);
        Map<String,Object> title3 = new LinkedHashMap<>();
        title3.put("field",index+"_cwCount");
        title3.put("title","错误量");
        title3.put("align","center");
        title3.put("width",150);
        list.add(title3);
        return list;
    }

    private List<Map<String,Object>> getTitle2(){
        List<Map<String,Object>> list = new ArrayList<>();
        Map<String,Object> title0 = new LinkedHashMap<>();
        title0.put("field", "province");
        title0.put("title","省直");
        title0.put("align","center");
        title0.put("width",150);
        list.add(title0);
        Map<String,Object> title1 = new LinkedHashMap<>();
        title1.put("field","sx_fz");
        title1.put("title","涉及监管事项主项数量\n(覆盖率分子)(不含采集系统)");
        title1.put("align","center");
        title1.put("width",150);
        list.add(title1);
        Map<String,Object> title2 = new LinkedHashMap<>();
        title2.put("field","fgl_fm");
        title2.put("title","监管事项主项总数（省直部门)(覆盖率分母）");
        title2.put("align","center");
        title2.put("width",150);
        list.add(title2);
        Map<String,Object> title3 = new LinkedHashMap<>();
        title3.put("field","jgsx_fgl");
        title3.put("title","监管行为涉及的监管事项主项覆盖率(省直部门)(不含采集系统)");
        title3.put("align","center");
        title3.put("width",150);
        list.add(title3);
        Map<String,Object> title4 = new LinkedHashMap<>();
        title4.put("field","jgsx_jsl");
        title4.put("title","监管业务数据上报及时率");
        title4.put("align","center");
        title4.put("width",150);
        list.add(title4);
        return list;
    }

    private List<Map<String,Object>> getTitle3(){
        List<Map<String,Object>> list = new ArrayList<>();
        Map<String,Object> title0 = new LinkedHashMap<>();
        title0.put("field", "city");
        title0.put("title","地市");
        title0.put("align","center");
        title0.put("width",150);
        list.add(title0);
        Map<String,Object> title1 = new LinkedHashMap<>();
        title1.put("field","city_fz");
        title1.put("title","涉及监管事项主项数量(市级)(覆盖率分子)");
        title1.put("align","center");
        title1.put("width",150);
        list.add(title1);
        Map<String,Object> title2 = new LinkedHashMap<>();
        title2.put("field","county_fz");
        title2.put("title","涉及监管事项主项数量(县/区)(覆盖率分子)");
        title2.put("align","center");
        title2.put("width",150);
        list.add(title2);

        Map<String,Object> title3 = new LinkedHashMap<>();
        title3.put("field","zrr_fz");
        title3.put("title","自然人分子");
        title3.put("align","center");
        title3.put("width",150);
        list.add(title3);
        Map<String,Object> title4 = new LinkedHashMap<>();
        title4.put("field","fr_fz");
        title4.put("title","法人分子");
        title4.put("align","center");
        title4.put("width",150);
        list.add(title4);


        Map<String,Object> title5 = new LinkedHashMap<>();
        title5.put("field","fgl_fm");
        title5.put("title","监管事项主项总数(市级)(覆盖率分母)");
        title5.put("align","center");
        title5.put("width",150);
        list.add(title5);
        Map<String,Object> title6 = new LinkedHashMap<>();
        title6.put("field","fgl_qx_fm");
        title6.put("title","监管事项主项总数(县/区)(覆盖率分母)");
        title6.put("align","center");
        title6.put("width",150);
        list.add(title6);

        Map<String,Object> title7 = new LinkedHashMap<>();
        title7.put("field","zrr_fm");
        title7.put("title","自然人分母");
        title7.put("align","center");
        title7.put("width",150);
        list.add(title7);
        Map<String,Object> title8 = new LinkedHashMap<>();
        title8.put("field","fr_fm");
        title8.put("title","法人分母");
        title8.put("align","center");
        title8.put("width",150);
        list.add(title8);

        Map<String,Object> title9 = new LinkedHashMap<>();
        title9.put("field","city_fgl");
        title9.put("title"," 监管行为涉及的监管事项主项覆盖率(市级)(不含采集系统)");
        title9.put("align","center");
        title9.put("width",150);
        list.add(title9);
        Map<String,Object> title10 = new LinkedHashMap<>();
        title10.put("field","county_fgl");
        title10.put("title"," 监管行为涉及的监管事项主项覆盖率(县/区级平均)(不含采集系统)");
        title10.put("align","center");
        title10.put("width",150);
        list.add(title10);

        Map<String,Object> title11 = new LinkedHashMap<>();
        title11.put("field","zrr_fgl");
        title11.put("title"," 自然人覆盖率");
        title11.put("align","center");
        title11.put("width",150);
        list.add(title11);
        Map<String,Object> title12 = new LinkedHashMap<>();
        title12.put("field","fr_fgl");
        title12.put("title"," 法人覆盖率");
        title12.put("align","center");
        title12.put("width",150);
        list.add(title12);


        Map<String,Object> title13 = new LinkedHashMap<>();
        title13.put("field","sbjsl");
        title13.put("title","监管业务数据上报及时率");
        title13.put("align","center");
        title13.put("width",150);
        list.add(title13);
        return list;
    }
}
