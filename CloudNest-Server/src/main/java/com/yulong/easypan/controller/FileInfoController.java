package com.yulong.easypan.controller;

import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.enums.FileCategoryEnums;
import com.yulong.easypan.entity.enums.FileDelFlagEnums;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.result.UploadResultVO;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.service.FileInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 文件信息Controller
 */
@Api(tags = "文件操作")
@RestController("fileInfoController")
@RequestMapping("file/")
public class FileInfoController extends ABaseController {
    @Autowired
    private FileInfoService fileInfoService;

    /**
     * 根据条件分类查询
     */
    @RequestMapping("loadDataList")
    //@GlobalInterceptor
    @ApiOperation("文件分页查询")
    public ResponseVO LoadDateList(HttpSession session, FileInfoQuery query,String category){

        FileCategoryEnums fileCategoryEnums = FileCategoryEnums.getByCode(category);
        if(fileCategoryEnums != null){
            query.setFileCategory(fileCategoryEnums.getCategory());
        }
        //query.setUserId(getUserInfoSession(session).getUserId());
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("last_update_time desc");
        PaginationResultVO result = fileInfoService.LoadDateList(query);
        return getSuccessResponseVO(result);
    }

    /**
     *文件分片上传接口
     * @param session
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid 文件所属的父级目录
     * @param fileMd5 文件的md5
     * @param chunkIndex 当前分片的编号
     * @param chunks 总分片数
     * @return
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 String fileName,
                                 String filePid,
                                 String fileMd5,
                                 Integer chunkIndex,
                                 Integer chunks){
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        UploadResultVO uploadResultVO = fileInfoService.upLoadFile(webUserDto,fileId,file,fileName,filePid,fileMd5,chunkIndex,chunks);
        return getSuccessResponseVO(uploadResultVO);
    }
}
