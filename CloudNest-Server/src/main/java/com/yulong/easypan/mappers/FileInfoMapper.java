package com.yulong.easypan.mappers;

import com.github.pagehelper.Page;
import com.yulong.easypan.entity.pjo.FileInfo;
import com.yulong.easypan.entity.query.FileInfoQuery;
import com.yulong.easypan.entity.vo.FileInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileInfoMapper {
    Page<FileInfoVO> LoadDateList(FileInfoQuery query);

    Long selectUserSpace(String userId);

    List<FileInfo> selectAllfileinfo(FileInfoQuery infoQuery);

    void insert(FileInfo fileInfo);

    FileInfo selectByUserAndFileId(String fileId, String userId);

    void updateStautusAndCover(String file_Id, String user_Id, FileInfo updatefile, Integer oldstatus);

    void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo,
                                @Param("userId") String userId,
                                @Param("filePidList") List<String> filePidList,
                                @Param("fileIdList") List<String> fileIdList,
                                @Param("oldDelFlag") Integer oldDelFlag);

    @Select("select sum(file_size)  from file_info where user_id = #{userId}")
    Long selectUseSpace(String userId);

    void delFileBatch(@Param("userId") String userId,
                      @Param("filePidList") List<String> filePidList,
                      @Param("fileIdList") List<String> fileIdList,
                      @Param("oldDelFlag") Integer oldDelFlag);

    @Select("select * from file_info where file_id = #{fileId} and user_id = #{userId}")
    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

    void insertBatch(List<FileInfo> copyFileList);
}
