package com.yulong.easypan.service;

import com.sun.mail.imap.protocol.INTERNALDATE;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
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

}
