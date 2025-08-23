package com.yulong.easypan.mappers;

import com.github.pagehelper.Page;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.vo.FileInfoVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FileInfoMapper {
    Page<FileInfoVO> LoadDateList(FileInfoQuery query);

    Long selectUserSpace(String userId);

    List<FileInfo> selectAllfileinfo(FileInfoQuery infoQuery);

    void insert(FileInfo fileInfo);
}
