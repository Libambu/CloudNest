package com.yulong.easypan.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionShareDto;
import com.yulong.easypan.entity.enums.ResponseCodeEnum;
import com.yulong.easypan.entity.enums.ShareValidTypeEnums;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.pjo.FileShare;
import com.yulong.easypan.entity.query.FileShareQuery;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ShareInfoVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.mappers.FileInfoMapper;
import com.yulong.easypan.mappers.FileShareMapper;
import com.yulong.easypan.service.FileShareService;
import com.yulong.easypan.utils.DateUtil;
import com.yulong.easypan.utils.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class FileShareServiceImpl implements FileShareService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private FileShareMapper fileShareMapper;

    @Override
    public PaginationResultVO findListByPage(FileShareQuery query) {
        if(query.getPageNo()==null){
            query.setPageNo(1);
        }
        if(query.getPageSize()==null){
            query.setPageSize(10);
        }
        PageHelper.startPage(query.getPageNo(), query.getPageSize());
        Page<FileShare> page = fileShareMapper.LoadDateList(query);
        //获取总记录数
        Long total = page.getTotal();
        //获取当前页文件列表
        List<FileShare> list = page.getResult();
        //获取总页数
        Integer pagenum = page.getPages();

        PaginationResultVO resultVO = new PaginationResultVO<>();

        if(query.getQueryFileName()==true){
            for(FileShare fileShare : list){
                FileInfo fileInfo = fileInfoMapper.selectByUserAndFileId(fileShare.getFileId(), fileShare.getUserId());
                fileShare.setFileName(fileInfo.getFileName());
            }
        }

        resultVO.setPageSize(query.getPageSize());
        resultVO.setPageNo(query.getPageNo());
        resultVO.setList(list);
        resultVO.setTotalCount(total);
        resultVO.setPageTotal(pagenum);
        return resultVO;
    }

    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnum = ShareValidTypeEnums.getByType(share.getValidType());
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (typeEnum != ShareValidTypeEnums.FOREVER) {
            share.setExpireTime(DateUtil.getAfterDate(typeEnum.getDays()));
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if (share.getCode()==null || share.getCode().isEmpty()) {
            share.setCode(StringTools.getRandomNumber(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomNumber(20));
        share.setShowCount(0);
        this.fileShareMapper.insert(share);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, String userId) {
        Integer count = this.fileShareMapper.deleteFileShareBatch(shareIdArray, userId);
        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    @Override
    public ShareInfoVO getFileShareByShareId(String shareId) {

        ShareInfoVO fileShare =  fileShareMapper.getFileShareByShareId(shareId);
        return fileShare;
    }

    @Override
    public SessionShareDto  checkShareCode(String shareId, String code) {
        FileShare share = this.fileShareMapper.selectByShareId(shareId);
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        if (!share.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }

        //更新浏览次数
        this.fileShareMapper.updateShareShowCount(shareId);
        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareId(shareId);
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }
}
