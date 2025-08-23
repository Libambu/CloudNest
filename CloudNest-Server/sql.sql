
# 创建邮箱表
create table email_code
(
    email       varchar(150) not null comment '邮箱',
    code        varchar(5)   not null comment '验证码',
    create_time datetime     null comment '创建时间',
    status      tinyint      null comment '状态',
    constraint email_code_pk
        primary key (email, code)
)
    comment '邮箱发送验证码';

#文件信息表
create table file_info
(
    user_id          varchar(10)  not null comment '对应用户id',
    file_id          varchar(10)  not null comment '文件id',
    file_md5         varchar(32)  null,
    file_pid         varchar(10)  null comment '父级id',
    file_size        bigint       null comment '文件大小bite',
    file_name        varchar(200) null comment '文件名称',
    file_cover       varchar(100) null comment '文件封面(图片视频)',
    fie_path         varchar(100) null comment '文件存储位置',
    create_time      datetime     null,
    last_update_time datetime     null,
    folder_type      tinyint      null comment '0是文件1是目录',
    file_category    tinyint      null comment '文件分类 1是视频 2是音频 3是图片 4是文档 5是其他',
    file_type        tinyint      null comment '1:视频2:音频3:图片4:pdf5:doc6:execl7:txt 8:code 9zip 10:其他',
    status           tinyint      null comment ' 0:转码中1：转码失败2：转码成功',
    recover_time     datetime     null comment '进入回收站时间',
    del_flag         tinyint      null comment '标记删除 0删除1回收站2正常',
    constraint file_info_pk
        primary key (user_id, file_id)
)
    comment '文件上传';

create index file_info_create_time_index
    on file_info (create_time);

create index file_info_del_flag_index
    on file_info (del_flag);

create index file_info_file_md5_index
    on file_info (file_md5);

create index file_info_file_pid_index
    on file_info (file_pid);

create index file_info_recover_time_index
    on file_info (recover_time);

create index file_info_user_id_index
    on file_info (user_id);