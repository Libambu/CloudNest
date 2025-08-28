package com.yulong.easypan.service;

import com.sun.mail.imap.protocol.INTERNALDATE;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.result.UploadResultVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileInfoService {

    /**
     * 文件列表分页查询
     * @param query
     * @return
     */
    PaginationResultVO LoadDateList(FileInfoQuery query);

    /**
     * 文件切片上传
     * @param webUserDto
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param chunkIndex
     * @param chunks
     */
    UploadResultVO upLoadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    /**
     * 异步进行文件合并
     * @param fileId
     * @param sessionWebUserDto
     */
    public void transferFile(String fileId,SessionWebUserDto sessionWebUserDto);

    FileInfo newFolder(String fileId, String userId, String folderName);

    FileInfo rename(String fileId, String userId, String fileName);

    void changeFileFolder(String fileIds, String filePid, String userId);

    void removeFile2RecycleBatch(String userId, String fileIds);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, Boolean b);

    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

    void checkRootFilePid(String fileId, String shareUserId, String filePid);

    void saveShare(String fileId, String shareFileIds, String myFolderId, String shareUserId, String userId);
}
