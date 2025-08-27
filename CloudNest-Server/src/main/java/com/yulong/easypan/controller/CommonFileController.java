package com.yulong.easypan.controller;

import com.yulong.easypan.config.appConfig;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.enums.FileCategoryEnums;
import com.yulong.easypan.entity.enums.FileFolderTypeEnums;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.mappers.FileInfoMapper;
import com.yulong.easypan.service.FileInfoService;
import com.yulong.easypan.utils.StringTools;
import io.swagger.models.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

@RestController
public class CommonFileController extends ABaseController{
    @Autowired
    private appConfig appConfig;
    @Autowired
    private FileInfoMapper fileInfoMapper;

    public void getImage(HttpServletResponse response,String imageFolder,String imageName){
        if(imageFolder.isEmpty()||imageName.isEmpty()){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String path = appConfig.getBasePath() + Constants.FILE_FOLDER_FILE + imageFolder + "/"  + imageName;
        String imageSuffixNoDot = imageSuffix.replace(".","");
        String contentType = "image/" + imageSuffixNoDot;
        response.setContentType(contentType);
        response.setHeader("Cache-Control","max-age=259200");
        readFile(response,path);
    }

    protected void getFile(HttpServletResponse response, String fileId, String userId) {


        String fileFolder = null;
        String finalPath = null;

        if(fileId.endsWith(".ts")){
            String[] list = fileId.split("_");
            String realFileId = list[0];
            FileInfo fileInfo = fileInfoMapper.selectByUserAndFileId(realFileId, userId);
            if(fileInfo==null){
                return;
            }
            // 202508//550034669266160_0296151752
            String fileWithMonth = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
            // E:\program\workspace\CloudNest\CloudNest-Server\file\
            fileFolder=appConfig.getBasePath() + Constants.FILE_FOLDER_FILE;
            // E:\program\workspace\CloudNest\CloudNest-Server\file\202508\550034669266160_0296151752\file_id_0000
            finalPath = fileFolder + fileWithMonth + "\\" + fileId;

        }else {
            FileInfo fileInfo = fileInfoMapper.selectByUserAndFileId(fileId, userId);
            if(fileInfo == null){
                return;
            }
            //如果是视频文件需要把数据库中存的mp4地址转为ts地址
            if(fileInfo.getFileCategory().equals(FileCategoryEnums.VIDEO.getCategory())){
                // 202508//550034669266160_0296151752
                String fileWithMonth = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                // E:\program\workspace\CloudNest\CloudNest-Server\file\
                fileFolder=appConfig.getBasePath() + Constants.FILE_FOLDER_FILE;
                // E:\program\workspace\CloudNest\CloudNest-Server\file\202508\550034669266160_0296151752\index.m3u8
                finalPath = fileFolder + fileWithMonth + "\\" + Constants.M3U8_NAME;
            }else {
                finalPath = appConfig.getBasePath() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
        }
        File file = new File(finalPath);
        if(!file.exists()){
            return;
        }
        readFile(response,finalPath);
    }


    public ResponseVO getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        String orderBy = "field(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoMapper.selectAllfileinfo(infoQuery);
        return getSuccessResponseVO(fileInfoList);
    }

}
