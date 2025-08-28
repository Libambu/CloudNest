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
import com.yulong.easypan.entity.pjo.UserInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.result.UploadResultVO;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.mappers.FileInfoMapper;
import com.yulong.easypan.mappers.userInfoMapper;
import com.yulong.easypan.service.FileInfoService;
import com.yulong.easypan.utils.DateUtil;
import com.yulong.easypan.utils.ProcessUtils;
import com.yulong.easypan.utils.ScaleFilter;
import com.yulong.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileInfoServiceImpl implements FileInfoService {

    @Autowired
    @Lazy//防止循环依赖
    private FileInfoService fileInfoService;

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
        updateUserSpace(webUserDto,userSpaceDto.getUseSpace() + totalSize);
        resultVO.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());





        //过程中有实物，所以要在事务结束之后开始该步骤，整个事务提交后再去调用这个方法
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //此处开始异步线程，进行文件合并
                //诶黑这里写fileID就会报错，因为是局部变量，不是final
                fileInfoService.transferFile(fileInfo.getFileId(),webUserDto);
            }
        });


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
    //文件合并
    @Async
    public void transferFile(String fileId,SessionWebUserDto sessionWebUserDto){
        Boolean transferSuccess = true;
        String targetFilePath=null,cover=null;
        FileTypeEnums fileTypeEnums = null;
        FileInfo fileInfo = this.fileInfoMapper.selectByUserAndFileId(fileId,sessionWebUserDto.getUserId());


        try{
            if(fileInfo==null||!fileInfo.getStatus().equals(FileStatusEnums.TRANSFER.getStatus())){
                return;
            }else{
                //然后打开临时目录进行文件整合
                String tempFolder = appConfig.getBasePath() + "\\" + Constants.FILE_FOLDER_TEMP;
                String currentUserFolderName = sessionWebUserDto.getUserId() + "_" + fileId;
                //创建对应分片的文件夹
                File fileFoder = new File(tempFolder+currentUserFolderName);
                String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
                //目标目录
                String Month = DateUtil.format(fileInfo.getCreateTime(),DateTimePatternEnum.YYYYMM.getPattern());
                String targetPath = appConfig.getBasePath() + Constants.FILE_FOLDER_FILE;
                File targetFolder = new File(targetPath+Month);
                if(!targetFolder.exists()){
                    targetFolder.mkdirs();
                }
                //创建好对应的目录，然后要再目录中写入文件
                //真实文件名
                String realName = currentUserFolderName+fileSuffix;
                targetFilePath = targetFolder.getPath() + "//" + realName;
                //fileFoder为存放分片的文件夹，targetFilePath目标文件,然后现在直接合并文件就OK
                union(fileFoder.getPath(),targetFilePath,fileInfo.getFileName(),true);
                //TODO 视频文件切割cover
                fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
                if(FileTypeEnums.VIDEO.equals(fileTypeEnums)){
                    cutFile4Video(fileId,targetFilePath);
                    //视频生成缩略图
                    cover = Month + "//" + currentUserFolderName + Constants.IMAGE_PNG;
                    String coverPath = targetPath + cover;
                    //targetPath为视频文件，coverPath是要存放封面的目录，150是封面宽度
                    ScaleFilter.createCover4Video( new File(targetFilePath),Constants.LENGTH_150,new File(coverPath));
                }else if(FileTypeEnums.IMAGE.equals(fileTypeEnums)){
                    //_.就是缩略图的位置
                    cover = Month + "//" +realName.replace(".","_.");
                    String coverPath = targetPath+cover;
                    Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath),Constants.LENGTH_150,new File(coverPath),false);
                    if(!created){
                        //如果原图本来就很小就不会生成缩率图，直接copy即可
                        FileUtils.copyFile(new File(targetFilePath),new File(coverPath));
                    }
                }

            }
        } catch (Exception e) {
            log.error("文件转码失败，文件ID:{},userID:{}",fileId,sessionWebUserDto.getUserId());
            transferSuccess = false;
        }finally {
             FileInfo updatefile = new FileInfo();
             updatefile.setFileSize(new File(targetFilePath).length());
             updatefile.setFileCover(cover);
             updatefile.setStatus(transferSuccess?FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER.getStatus());
             fileInfoMapper.updateStautusAndCover(fileId,sessionWebUserDto.getUserId(),updatefile,FileStatusEnums.TRANSFER.getStatus());

        }

    }



    private void union(String firPath,String toFilePath,String fileName,Boolean delSource) throws Exception {
        File dir = new File(firPath);
        if(!dir.exists()){
            throw new BusinessException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writer = null;

        try{
            writer = new RandomAccessFile(targetFile,"rw");
            byte[] b = new byte[1024*10];
            for(int i=0;i<fileList.length;i++){
                int len = -1;
                File chunkFile = new File(firPath + "//" + i);
                RandomAccessFile read = new RandomAccessFile(chunkFile,"r");
                try{
                    while((len=read.read(b))!=-1){
                        writer.write(b,0,len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败");
                    throw new BusinessException("合并分片失败");
                }finally {
                    read.close();
                }
            }
        } catch (Exception e) {
            log.error(fileName+"  合并分片失败");
            throw new BusinessException("合并分片失败");
        }finally {
            writer.close();
            if(delSource&& dir.exists()){
                FileUtils.deleteDirectory(dir);
            }
        }
    }
    //这段代码“化整为零”：先把 mp4 转成一个完整的 .ts，再把它切成 30 秒一片的 .ts 并生成 .m3u8，最后把中间那个大 .ts 删掉，只留下 HLS 切片。
    //会将这个abc.mp4文件转成abc/u3m8 ts
    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        //把原始 MP4 文件无损地“换壳”成一个完整的 .ts 文件（临时文件）
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy  %s";
        //将index.ts进行切片
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "//" + Constants.TS_NAME;
        //生成.ts 把带占位符的字符串模板替换成真正的值，生成最终要用的字符串。
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8 和切片.ts
        //它会按 30 秒一段 把 index.ts 切成若干小块，文件名固定用
        //{fileId}_0000.ts、{fileId}_0001.ts、{fileId}_0002.ts … 依次递增，4 位数字，补零。
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    @Override
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid,userId,folderName,FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomNumber(10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = this.fileInfoMapper.selectByUserAndFileId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        if (fileInfo.getFileName().equals(fileName)) {
            return fileInfo;
        }
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        //文件获取后缀
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date curDate = new Date();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileName(fileName);
        dbInfo.setLastUpdateTime(curDate);
        fileInfoMapper.updateStautusAndCover(fileId, userId,dbInfo,dbInfo.getStatus());

        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfos = fileInfoMapper.selectAllfileinfo(fileInfoQuery);
        if (fileInfos.size() > 1) {
            throw new BusinessException("文件名" + fileName + "已经存在");
        }
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    @Override
    public void changeFileFolder(String fileIds, String filePid, String userId) {

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectAllfileinfo(query);
        if (fileInfoList.isEmpty()) {
            return;
        }
        //删除的可能是文件也有可能是目录，所以要把子目录给查出来 目录+文件
        List<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(delFilePidList, userId, fileInfo.getFileId(), FileDelFlagEnums.USING.getFlag());
        }
        //将目录下的所有文件更新为已删除
        if (!delFilePidList.isEmpty()) {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(updateInfo, userId, delFilePidList, null, FileDelFlagEnums.USING.getFlag());
        }

        //将选中的文件更新为回收站
        //删除后所有放回收站，但是只有最高级（页面选中的标记回收站，下层文件标记删除）
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlagEnums.USING.getFlag());
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectAllfileinfo(query);

        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //查询所有跟目录的文件,因为恢复的文件都要放根目录所以要查，防止名字重复
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFilePid("0");
        List<FileInfo> allRootFileList = this.fileInfoMapper.selectAllfileinfo(query);
        //key为fileName value是fileInfo
        Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        //查询所有所选文件
        //将目录下的所有删除的文件更新为正常
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileSubFolderFileIdList, null, FileDelFlagEnums.DEL.getFlag());
        }
        //将选中的文件更新为正常,且父级目录到跟目录
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid("0");
        fileInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlagEnums.RECYCLE.getFlag());

        //将所选文件重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            //文件名已经存在，重命名被还原的文件名
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                fileInfoMapper.updateStautusAndCover(item.getFileId(),userId,updateInfo,2);
            }
        }
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        if(!adminOp){
            query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        }
        List<FileInfo> fileInfoList = fileInfoMapper.selectAllfileinfo(query);
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }

        //删除所选文件，子目录中的文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            this.fileInfoMapper.delFileBatch(userId, delFileSubFolderFileIdList, null, adminOp ? null : FileDelFlagEnums.DEL.getFlag());
        }
        //删除所选文件
        this.fileInfoMapper.delFileBatch(userId, null, Arrays.asList(fileIdArray), adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());

        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserSpace(useSpace);
        userInfoMapper.updateUserSpace(userId,useSpace,null);

        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);

    }

    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        FileInfo fileInfo = fileInfoMapper.getFileInfoByFileIdAndUserId(fileId,userId);
        return fileInfo;
    }

    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if (fileId.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(fileId)) {
            return;
        }
        checkFilePid(rootFilePid, fileId, userId);
    }

    @Override
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String cureentUserId) {
        String[] shareFileIdArray = shareFileIds.split(",");
        //目标目录文件列表
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(cureentUserId);
        fileInfoQuery.setFilePid(myFolderId);
        List<FileInfo> currentFileList = this.fileInfoMapper.selectAllfileinfo(fileInfoQuery);
        Map<String, FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        //选择的文件
        fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(shareUserId);
        fileInfoQuery.setFileIdArray(shareFileIdArray);
        List<FileInfo> shareFileList = this.fileInfoMapper.selectAllfileinfo(fileInfoQuery);
        //重命名选择的文件
        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if (haveFile != null) {
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList, item, shareUserId, cureentUserId, curDate, myFolderId);
        }
        System.out.println(copyFileList.size());
        this.fileInfoMapper.insertBatch(copyFileList);
    }

    private void findAllSubFile(List<FileInfo> copyFileList, FileInfo fileInfo, String sourceUserId, String currentUserId, Date curDate, String newFilePid) {
        String sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(currentUserId);
        String newFileId = StringTools.getRandomNumber(10);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(sourceFileId);
            query.setUserId(sourceUserId);
            List<FileInfo> sourceFileList = this.fileInfoMapper.selectAllfileinfo(query);
            for (FileInfo item : sourceFileList) {
                findAllSubFile(copyFileList, item, sourceUserId, currentUserId, curDate, newFileId);
            }
        }
    }

    private void checkFilePid(String rootFilePid, String fileId, String userId) {
        FileInfo fileInfo = this.fileInfoMapper.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if ("0".equals(fileInfo.getFilePid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfos = fileInfoMapper.selectAllfileinfo(fileInfoQuery);
        if (fileInfos.size() > 0) {
            throw new BusinessException("此目录下已存在同名文件，请修改名称");
        }
    }

    //递归 回溯查目录
    private void findAllSubFolderFileIdList(List<String> fileIdList, String userId, String fileId, Integer delFlag) {
        fileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(fileId);
        query.setDelFlag(delFlag);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectAllfileinfo(query);
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(fileIdList, userId, fileInfo.getFileId(), delFlag);
        }
    }
}
