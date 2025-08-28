package com.yulong.easypan.service;


import com.yulong.easypan.entity.dto.SessionShareDto;
import com.yulong.easypan.entity.pjo.FileShare;
import com.yulong.easypan.entity.query.FileShareQuery;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ShareInfoVO;

public interface FileShareService {
    PaginationResultVO findListByPage(FileShareQuery query);

    void saveShare(FileShare share);

    void deleteFileShareBatch(String[] split, String userId);

    ShareInfoVO getFileShareByShareId(String shareId);

    SessionShareDto checkShareCode(String shareId, String code);
}
