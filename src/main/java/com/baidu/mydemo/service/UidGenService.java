package com.baidu.mydemo.service;

import com.baidu.fsg.uid.UidGenerator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@Service
public class UidGenService {
    //    @Resource(name = "cachedUidGenerator")
    @Resource(name = "defaultUidGenerator")
    private UidGenerator uidGenerator;

    /**
     * 生成指定个百度uid
     *
     * @param num 请求生成的id数
     * @return java.util.List
     * @date 2020/7/31 9:40
     */
    public List getUid(int num) {
        List list = new ArrayList<>();
        for (int i = num; i > 0; i--) {
            long uid = uidGenerator.getUID();
            list.add(uidGenerator.parseUID(uid));
        }
        return list;
    }


}
