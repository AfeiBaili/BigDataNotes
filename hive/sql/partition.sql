show databases;

use myhive;

-- 创建分区表
create table my_partitioned
(
    id       int,
    name     string,
    location string
)
    partitioned by (day string)
    row format delimited fields terminated by '\t';

show tables;

-- 从hdfs加载到分区表
load data inpath '/datafile'
    into table my_partitioned
    partition (day = "20241106");

select *
from my_partitioned;

insert overwrite table my_partitioned
    partition (day = "20241107")
select id, name, location
from my_partitioned
where day = '20241106';

-- 二级分区
create table tow_partitioned
(
    id       int,
    name     string,
    location string
) partitioned by (day string,hour string);


insert overwrite table tow_partitioned
    partition (day = '20241106', hour = '01')
select id, name, location
from my_partitioned
where day = '20241106';

select *
from tow_partitioned;

load data inpath '/datafile'
    into table tow_partitioned
    partition (day = '20241106', hour = '02');

show partitions tow_partitioned;

-- 动态分区

set hive.exec.dynamic