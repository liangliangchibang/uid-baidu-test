# 1.简单说明

该项目主要是将百度雪花算法[uid-generator](https://github.com/baidu/uid-generator)打成jar包引入到项目中，目前该项目引入的是升级后的uid-generator【之前的version比较老，所以进行了升级】，升级为2.0.0-SNAPSHOT，之前的可看做1.0.0-SNAPSHOT，这两个版本都在本地进行了install，唯一不同就是version（uid-generator自身引用的version不同），在使用过程中可以相互切换，目前没有发现有什么影响。

## 2.使用说明

关于引用改代码部分为两块：test 和 业务代码，下面分别说明

注：该项目使用的是mysql数据库，Oracle数据库请自行测试

## 2.1 test测试说明

该项目主要针对默认方法和缓存方法进行测试，其最大区别是每次默认方法每次都是一次新的请求，缓存方法是一次请求供多次调用。

test中的测试源码来源于uid-generator项目中的test部分，除了数据库连接有改动其他地方未曾变更。这部分注意test方法的写法，值得学习一下，包括引入的unit jar包和版本。



## 2.2 业务代码

业务代码来源于[java-spring-boot-uid-generator-baidu](https://github.com/foxiswho/java-spring-boot-uid-generator-baidu)中的uid-provider部分，实质上这个（[java-spring-boot-uid-generator-baidu](https://github.com/foxiswho/java-spring-boot-uid-generator-baidu)）项目就是springboot整合百度uid-generator的，正是基于此做的改变，它是将其作为一个服务模块引入，而我是将其打成了jar包进行引入，相当于比他多走了一步。

需要注意的地方是几个注解部分：①启动项中的扫描注解；

②在config中配置一个类。注意这里好像不能同时引入两个文件，尝试过失败过。

```
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(locations = {"classpath:uid/default-uid-spring.xml"})
public class UidConfig {
}

```

③service中的方法要指明使用的是哪种方式

```
import com.baidu.fsg.uid.UidGenerator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class UidGenService {
    // @Resource(name = "cachedUidGenerator")
    @Resource(name = "defaultUidGenerator")
    private UidGenerator uidGenerator;

    public long getUid() {
        return uidGenerator.getUID();
    }
}
```

项目到此基本上就ok了，详情看源码和结构