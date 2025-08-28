package com.yulong.easypan.controller;


import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionShareDto;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.enums.FileDelFlagEnums;
import com.yulong.easypan.entity.enums.ResponseCodeEnum;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.pjo.FileShare;
import com.yulong.easypan.entity.pjo.UserInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.entity.vo.ShareInfoVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.service.FileInfoService;
import com.yulong.easypan.service.FileShareService;
import com.yulong.easypan.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController{

    @Autowired
    private FileShareService fileShareService;
    @Autowired
    private FileInfoService fileInfoService;
    @Autowired
    private UserInfoService userInfoService;


    /**
     * 获取分享登录信息
     *
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareLoginInfo")
    public ResponseVO getShareLoginInfo(HttpSession session,  String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            return getSuccessResponseVO(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是否是当前用户分享的文件
        SessionWebUserDto userDto = getUserInfoSession(session);
        if (userDto != null && userDto.getUserId().equals(shareSessionDto.getShareUserId())) {
            //如果是自己分享的会有取消分享按钮
            shareInfoVO.setCurrentUser(true);
        } else {
            shareInfoVO.setCurrentUser(false);
        }
        return getSuccessResponseVO(shareInfoVO);
    }

    /**
     * 获取分享信息
     *
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareInfo")
    public ResponseVO getShareInfo( String shareId) {
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    private ShareInfoVO getShareInfoCommon(String shareId) {
        ShareInfoVO shareInfoVO = fileShareService.getFileShareByShareId(shareId);
        if (null == shareInfoVO || (shareInfoVO.getExpireTime() != null && new Date().after(shareInfoVO.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(shareInfoVO.getFileId(), shareInfoVO.getUserId());
        if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getUserInfoByUserId(shareInfoVO.getUserId());
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setAvatar(userInfo.getQqAvatar());
        shareInfoVO.setUserId(userInfo.getUserId());
        return shareInfoVO;
    }

    /**
     * 校验分享码
     *
     * @param session
     * @param shareId
     * @param code
     * @return
     */
    @RequestMapping("/checkShareCode")
    public ResponseVO checkShareCode(HttpSession session,
                                     String shareId,
                                     String code) {
        SessionShareDto shareSessionDto = fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, shareSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取文件列表
     *
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/loadFileList")
    public ResponseVO loadFileList(HttpSession session,
                                  String shareId, String filePid) {

        SessionShareDto shareSessionDto = checkShare(session, shareId);
        FileInfoQuery query = new FileInfoQuery();
        if (!filePid.isEmpty() && !"0".equals(filePid)) {
            //椒盐查看的文件是否是分享文件夹的子文件 shareSessionDto.getFileId()是根目录下的id filePid是选中文件id 要判断后id在前id里头
            fileInfoService.checkRootFilePid(shareSessionDto.getFileId(), shareSessionDto.getShareUserId(), filePid);
            query.setFilePid(filePid);
        } else {
            query.setFileId(shareSessionDto.getFileId());
        }
        query.setUserId(shareSessionDto.getShareUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        //在分页查询中会根据filePid拿出文件夹下的文件，所以如果穿了一个坏的pid就会套出其他文件
        PaginationResultVO resultVO = fileInfoService.LoadDateList(query);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 校验分享是否失效
     *
     * @param session
     * @param shareId
     * @return
     */
    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        if (shareSessionDto.getExpireTime() != null && new Date().after(shareSessionDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return shareSessionDto;
    }
    /**
     * 获取目录层级 test1folder//test2folder
     * @param session
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor
    public ResponseVO getFolderInfo(HttpSession session,String path,String shareId){
        SessionShareDto sessionShareDto = checkShare(session,shareId);
        return super.getFolderInfo(path,sessionShareDto.getShareUserId());
    }

    /**
     * 获取文件
     * @param response
     * @param session
     * @param shareId
     * @param fileId
     */
    @RequestMapping("/getFile/{shareId}/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session,
                        @PathVariable("shareId") String shareId,
                        @PathVariable("fileId")  String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
                             HttpSession session,
                             @PathVariable("shareId")  String shareId,
                             @PathVariable("fileId")   String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")

    public ResponseVO createDownloadUrl(HttpSession session,
                                        @PathVariable("shareId")  String shareId,
                                        @PathVariable("fileId")   String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.createDownloadUrl(fileId, shareSessionDto.getShareUserId());
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code")  String code) throws Exception {
        super.download(request, response, code);
    }


    /**
     * 保存分享
     *
     * @param session
     * @param shareId
     * @param shareFileIds
     * @param myFolderId
     * @return
     */
    @RequestMapping("/saveShare")
    @GlobalInterceptor
    public ResponseVO saveShare(HttpSession session,
                                String shareId,
                                String shareFileIds,
                                String myFolderId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        if (shareSessionDto.getShareUserId().equals(webUserDto.getUserId())) {
            throw new BusinessException("自己分享的文件无法保存到自己的网盘");
        }
        fileInfoService.saveShare(shareSessionDto.getFileId(), shareFileIds, myFolderId, shareSessionDto.getShareUserId(), webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

}
