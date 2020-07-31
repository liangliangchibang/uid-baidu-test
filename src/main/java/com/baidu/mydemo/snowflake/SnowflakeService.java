package com.baidu.mydemo.snowflake;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 原始雪花算法方法调用
 *
 * @author lvhongwei
 * @version 1.0
 * @since 2020/7/31 10:18
 */
@Service
public class SnowflakeService {

    public List getUid(int num) {
        List list = new ArrayList<>();
        SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);

        for (int i = 0; i < num; i++) {
            long id = idWorker.nextId();
            System.out.println(Long.toBinaryString(id));
            System.out.println(id);
            list.add(idWorker.parseUID(id,Long.toBinaryString(id)));

        }

        return list;
    }

}
