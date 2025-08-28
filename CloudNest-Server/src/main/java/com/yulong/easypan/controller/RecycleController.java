package com.yulong.easypan.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.enums.FileDelFlagEnums;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.PaginationResultVO;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.mappers.FileInfoMapper;
import com.yulong.easypan.service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController {

    @Resource
    private FileInfoService fileInfoService;
    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor
    public ResponseVO loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
        if(pageNo==null){
            pageNo = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        FileInfoQuery query = new FileInfoQuery();
        query.setPageSize(pageSize);
        query.setPageNo(pageNo);
        query.setUserId(getUserInfoSession(session).getUserId());
        query.setOrderBy("recovery_time desc");
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        PageHelper.startPage(pageNo,pageSize);

        Page<FileInfoVO> page = fileInfoMapper.LoadDateList(query);
        //获取总记录数
        Long total = page.getTotal();
        //获取当前页文件列表
        List<FileInfoVO> list = page.getResult();
        //获取总页数
        Integer pagenum = page.getPages();
        PaginationResultVO resultVO = new PaginationResultVO<>();

        resultVO.setPageSize(query.getPageSize());
        resultVO.setPageNo(query.getPageNo());
        resultVO.setList(list);
        resultVO.setTotalCount(total);
        resultVO.setPageTotal(pagenum);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/recoverFile")
    @GlobalInterceptor
    public ResponseVO recoverFile(HttpSession session,  String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(HttpSession session,  String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        fileInfoService.delFileBatch(webUserDto.getUserId(), fileIds,false);
        return getSuccessResponseVO(null);
    }
}

