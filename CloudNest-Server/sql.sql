
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