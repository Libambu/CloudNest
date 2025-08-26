package com.yulong.easypan.controller;

import com.yulong.easypan.config.appConfig;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.utils.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class CommonFileController extends ABaseController{
    @Autowired
    private appConfig appConfig;

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

}
