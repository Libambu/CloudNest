package com.yulong.easypan.mappers;

import com.github.pagehelper.Page;
import com.yulong.easypan.entity.pjo.FileShare;
import com.yulong.easypan.entity.query.FileShareQuery;
import com.yulong.easypan.entity.vo.FileInfoVO;
import com.yulong.easypan.entity.vo.ShareInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileShareMapper {

    void insert(FileShare share);

    Page<FileShare> LoadDateList(FileShareQuery query);

    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);

    @Select("select * from file_share where share_id = #{shareId}")
    ShareInfoVO getFileShareByShareId(String shareId);

    @Select("select * from file_share where share_id = #{shareId}")
    FileShare selectByShareId(String shareId);

    void updateShareShowCount(String shareId);
}
