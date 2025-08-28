package com.yulong.easypan.controller;


import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.pjo.FileShare;
import com.yulong.easypan.entity.query.FileShareQuery;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.service.FileShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController("shareController")
@RequestMapping("/share")
public class ShareController extends ABaseController{
    @Autowired
    private FileShareService fileShareService;

    @RequestMapping("/loadShareList")
    @GlobalInterceptor
    public ResponseVO loadShareList(HttpSession session, FileShareQuery query) {
        query.setOrderBy("share_time desc");
        SessionWebUserDto userDto = getUserInfoSession(session);
        query.setUserId(userDto.getUserId());
        query.setQueryFileName(true);
        PaginationResultVO resultVO = this.fileShareService.findListByPage(query);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/shareFile")
    @GlobalInterceptor
    public ResponseVO shareFile(HttpSession session,
                                String fileId,
                                Integer validType,
                                String code) {
        SessionWebUserDto userDto = getUserInfoSession(session);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(userDto.getUserId());
        fileShareService.saveShare(share);
        return getSuccessResponseVO(share);
    }

    @RequestMapping("/cancelShare")
    @GlobalInterceptor
    public ResponseVO cancelShare(HttpSession session,  String shareIds) {
        SessionWebUserDto userDto = getUserInfoSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), userDto.getUserId());
        return getSuccessResponseVO(null);
    }

}
