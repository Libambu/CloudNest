package com.yulong.easypan.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yulong.easypan.component.RedisComponent;
import com.yulong.easypan.config.appConfig;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.dto.UserSpaceDto;
import com.yulong.easypan.entity.enums.*;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.result.UploadResultVO;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.mappers.FileInfoMapper;
import com.yulong.easypan.mappers.userInfoMapper;
import com.yulong.easypan.service.FileInfoService;
import com.yulong.easypan.utils.DateUtil;
import com.yulong.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileInfoServiceImpl implements FileInfoService {

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private userInfoMapper userInfoMapper;

    @Autowired
    private appConfig appConfig;
    /**
     * 进行分页查询
     * @param query
     * @return
     */
    @Override
    public PaginationResultVO LoadDateList(FileInfoQuery query) {
        PaginationResultVO resultVO = new PaginationResultVO<>();
        if(query.getPageNo()==null){
            query.setPageNo(1);
        }
        if(query.getPageSize()==null){
            query.setPageSize(10);
        }
        resultVO.setPageSize(query.getPageSize());
        resultVO.setPageNo(query.getPageNo());

        PageHelper.startPage(query.getPageNo(), query.getPageSize());
        Page<FileInfoVO> page = fileInfoMapper.LoadDateList(query);
        //获取总记录数
        Long total = page.getTotal();
        //获取当前页文件列表
        List<FileInfoVO> list = page.getResult();
        //获取总页数
        Integer pagenum = page.getPages();

        resultVO.setList(list);
        resultVO.setTotalCount(total);
        resultVO.setPageTotal(pagenum);
        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultVO upLoadFile(SessionWebUserDto webUserDto,
                                     String fileId,
                                     MultipartFile file,
                                     String fileName,
                                     String filePid,
                                     String fileMd5,
                                     Integer chunkIndex,
                                     Integer chunks) {
        //上传情况，出现异常删除temp文件夹
        Boolean successload = true;
        //开始操作文件上传
        UploadResultVO resultVO = new UploadResultVO();
        if(fileId.isEmpty()||fileId.length()==0){
            fileId = StringTools.getRandomNumber(10);
        }
        resultVO.setFileId(fileId);
        Date curDate = new Date();
        //开始判断用户空间是否可以放下当前分片
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(webUserDto.getUserId());
        //查询数据库中是否有该md5文件，实现秒传
        if(chunkIndex == 0){
            FileInfoQuery infoQuery = new FileInfoQuery();
            infoQuery.setFileMd5(fileMd5);
            infoQuery.setStatus(FileStatusEnums.USING.getStatus());
            List<FileInfo> dbFileList = fileInfoMapper.selectAllfileinfo(infoQuery);
            //查询到数据证明服务器中已经有该文件,直接copy数据库数据即可
            if(!dbFileList.isEmpty()){
                FileInfo fileInfo = dbFileList.get(0);
                if(userSpaceDto.getUseSpace()+fileInfo.getFileSize() > userSpaceDto.getTotalSpace()){
                    successload = false;
                    throw new BusinessException(ResponseCodeEnum.CODE_904);
                }
                fileInfo.setFileId(fileId);
                fileInfo.setUserId(webUserDto.getUserId());
                fileInfo.setFilePid(filePid);
                fileInfo.setCreateTime(curDate);
                fileInfo.setLastUpdateTime(curDate);
                fileInfo.setStatus(FileStatusEnums.USING.getStatus());
                fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
                fileInfo.setFileMd5(fileMd5);
                //文件重命名
                fileName = autoRename(filePid,webUserDto.getUserId(),fileName);
                fileInfo.setFileName(fileName);
                fileInfoMapper.insert(fileInfo);
                resultVO.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                //上传完后要更新用户空间
                updateUserSpace(webUserDto,userSpaceDto.getUseSpace()+fileInfo.getFileSize());
                return resultVO;
            }
        }
        //判断磁盘空间
        Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
        if(currentTempSize + file.getSize() + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()){
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        //暂存临时目录，服务器中没有文件则需要分片上传
        String tempFolderName = appConfig.getBasePath() + "\\" + Constants.FILE_FOLDER_TEMP;
        String currentUserFolderName = webUserDto.getUserId() + "_" + fileId;
        File tempFileFolder = new File(tempFolderName + currentUserFolderName);
        if(!tempFileFolder.exists()){
            tempFileFolder.mkdirs();
        }
        File newFile = new File(tempFileFolder.getPath()+"//"+chunkIndex);
        try{
            file.transferTo(newFile);
        } catch (Exception e) {
            log.error("文件上传失败");
            log.error(e.getMessage());
            successload = false;
            throw new RuntimeException(e);
        }finally {
            if(!successload){
                try{
                    FileUtils.deleteDirectory(tempFileFolder);
                }catch (Exception e){
                    log.error("删除临时目录失败");
                    log.error(e.toString());
                }
            }
        }
        //不是最后一个分片的时候执行
       if(chunkIndex<chunks-1){
           redisComponent.saveFileTempSize(webUserDto.getUserId(), fileId,file.getSize());
           resultVO.setStatus(UploadStatusEnums.UPLOADING.getCode());
           return resultVO;
       }
       //最后一个分片上传完成后，记录数据库，异步合并分片
        String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
        String fileSuffix = StringTools.getFileSuffix(fileName);
       //真是文件名
        String realFileName = currentUserFolderName + fileSuffix;
        FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
        //自动重新命名
        fileName = autoRename(filePid,webUserDto.getUserId(),fileName);
        //装填数据库
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(fileId);
        fileInfo.setUserId(webUserDto.getUserId());
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setFileName(fileName);
        fileInfo.setFilePath(month + "//" + realFileName);
        fileInfo.setFilePid(filePid);
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
        fileInfo.setFileType(fileTypeEnums.getType());
        fileInfo.setFileSize(redisComponent.getFileTempSize(webUserDto.getUserId(), fileId) + file.getSize());
        fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
        fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoMapper.insert(fileInfo);
        Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId) + file.getSize();
        updateUserSpace(webUserDto,totalSize);
        resultVO.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
        return resultVO;
    }
    //实现重命名
    private String autoRename(String filePid,String userId,String fileName){
        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFileName(fileName);
        List<FileInfo> fileInfos = fileInfoMapper.selectAllfileinfo(query);
        if(fileInfos != null){
            fileName = StringTools.rename(fileName);
        }
        return fileName;
    }
    //更新用户空间
    private void updateUserSpace(SessionWebUserDto webUserDto,Long useSpace){
        Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(),useSpace,null);
        UserSpaceDto spaceDto =  redisComponent.getUserSpaceDto(webUserDto.getUserId());
        spaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(), spaceDto);
    }
}
