package com.yulong.easypan.controller;

import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.enums.FileCategoryEnums;
import com.yulong.easypan.entity.enums.FileDelFlagEnums;
import com.yulong.easypan.entity.enums.FileFolderTypeEnums;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.result.UploadResultVO;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.mappers.FileInfoMapper;
import com.yulong.easypan.service.FileInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 文件信息Controller
 */
@Api(tags = "文件操作")
@RestController("fileInfoController")
@RequestMapping("file/")
public class FileInfoController extends CommonFileController {
    @Autowired
    private FileInfoMapper fileInfoMapper;
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
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    @GlobalInterceptor
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder,@PathVariable("imageName") String imageName){
        super.getImage(response,imageFolder,imageName);
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @GlobalInterceptor
    public void getVideoInfo(HttpServletResponse response,HttpSession session,@PathVariable("fileId") String fileId){
        SessionWebUserDto sessionWebUserDto = getUserInfoSession(session);
        super.getFile(response,fileId,sessionWebUserDto.getUserId());
    }

    @RequestMapping("/getFile/{fileId}")
    @GlobalInterceptor
    public void getFile(HttpServletResponse response,HttpSession session,@PathVariable("fileId") String fileId){
        SessionWebUserDto sessionWebUserDto = getUserInfoSession(session);
        super.getFile(response,fileId,sessionWebUserDto.getUserId());
    }

    /**
     * 新建目录
     * @param session
     * @param filePid
     * @param fileName
     * @return
     */
    @RequestMapping("/newFoloder")
    @GlobalInterceptor
    public ResponseVO newFolder(HttpSession session,String filePid,String fileName){
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        FileInfo fileInfo =  fileInfoService.newFolder(filePid,webUserDto.getUserId(),fileName);
        return getSuccessResponseVO(fileInfo);
    }

    /**
     * 获取目录层级 test1folder//test2folder
     * @param session
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor
    public ResponseVO getFolderInfo(HttpSession session,String path){
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        return super.getFolderInfo(path,webUserDto.getUserId());
    }

    /**
     * 文件重命名
     * @param session
     * @param fileName
     * @param fileId
     * @return
     */
    @RequestMapping("/rename")
    @GlobalInterceptor
    public ResponseVO rename(HttpSession session,String fileName,String fileId){
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId,webUserDto.getUserId(),fileName);
        return getSuccessResponseVO(fileInfo);
    }

    /**
     * 查询可移动文件夹
     * @param session
     * @param filePid
     * @param currentFileIds
     * @return
     */
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor()
    public ResponseVO loadAllFolder(HttpSession session, String filePid, String currentFileIds) {
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(getUserInfoSession(session).getUserId());
        query.setFilePid(filePid);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (!currentFileIds.isEmpty()) {
            query.setExcludeFileIdArray(currentFileIds.split(","));
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoMapper.selectAllfileinfo(query);
        return getSuccessResponseVO(fileInfoList);
    }

    /**
     * 文件移动
     * @param session
     * @param fileIds
     * @param filePid
     * @return
     */
    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor()
    public ResponseVO changeFileFolder(HttpSession session,
                                        String fileIds,
                                        String filePid) {
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
