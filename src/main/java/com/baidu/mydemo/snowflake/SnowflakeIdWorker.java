package com.baidu.mydemo.snowflake;

import com.baidu.fsg.uid.BitsAllocator;
import com.baidu.fsg.uid.utils.DateUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Twitter_Snowflake<br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截) 得到的值，
 * 这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。
 * 41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
 * 加起来刚好64位，为一个Long型。(转换成字符串后长度最多19)<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 * 补充： Unix时间戳(Unix timestamp)，或称Unix时间(Unix time)、POSIX时间(POSIX time)，是一种时间表示方式，定义为从格林威治时间1970年01月01日00时00分00秒起至现在的总秒数。
 * 32位表示的时候到2038年01月19日03时14分07秒就会溢出，二进制：01111111 11111111 11111111 11111111）。
 * 其后一秒，二进制数字会变为10000000 00000000 00000000 00000000，发生溢出错误，造成系统将时间误解为1901年12月13日20时45分52秒。这很可能会引起软件故障，甚至是系统瘫痪。
 * 使用64位二进制数字表示时间的系统（最多可以使用到格林威治时间292,277,026,596年12月04日15时30分08秒）则基本不会遇到这类溢出问题。
 *
 * @author LV
 * @date 2020/6/5
 */

public class SnowflakeIdWorker {

    /*** 开始时间截 (2015-01-01), 本算法从这个时间后推69年*/
    //private static final long START_STAMP = 1420041600000L;

    /*** 开始时间截 (2020-01-01), 本算法从这个时间后推69年*/
    private static final long START_STAMP = 1577808000000L;


    /*** 每一部分占用的位数*/

    /*** 序列在id中占的位数*/
    private final static long SEQUENCE_BIT = 12L;
    /*** 机器id所占的位数*/
    private final static long MACHINE_BIT = 5L;
    /*** 数据中心标识id所占的位数*/
    private final static long DATACENTER_BIT = 5L;


    /***知识点预热 原码，反码，补码
     * 异或 相同为0，不同为1
     * 同等效果写法还有  (1 << X) - 1 位数的最大值*/

    /*** 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)*/
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);
    /*** 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)*/
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    /*** 支持的最大数据标识id，结果是31*/
    private final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);


    /*** 每一部分向左的位移相加的和，现在暂时不明为啥要这样相加*/

    /*** 机器ID向左移12位*/
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    /*** 数据标识id向左移17位(12+5)*/
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    /*** 时间截向左移22位(5+5+12)*/
    private final static long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;


    /*** 数据中心ID(0~31)*/
    private long dataCenterId;
    /*** 工作机器ID(0~31)*/
    private long machineId;
    /*** 毫秒内序列(0~4095)*/
    private long sequence = 0L;
    /*** 上次生成ID的时间截*/
    private long lastStamp = -1L;


    /**
     * 构造函数
     * 最复杂的是工作机器id，该内容可查看百度和美团的相关实践，本文暂不拓展
     *
     * @param dataCenterId 数据中心ID (0~31)
     * @param machineId    工作ID (0~31)
     */
    public SnowflakeIdWorker(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATACENTER_NUM || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenterId can't be greater than %d or less than 0", MAX_DATACENTER_NUM));
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException(String.format("machineId can't be greater than %d or less than 0", MAX_MACHINE_NUM));
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }


    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        // 当前时间戳，毫秒
        long currentStmp = getNewTimeStmp();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过 这个时候应当抛出异常
        if (currentStmp < lastStamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastStamp - currentStmp));
        }

        // 固定位数之后，任意数值与最大数值进行&操作都会是它本身，
        // 用下列这种方式，使得sequence的方式永远不可能超过固定位数下的最大值
        // 于是则为   TIME10000-TIME19999 的一个数量区间
        //如果是同一时间生成的，则进行毫秒内序列
        if (currentStmp == lastStamp) {
            // 相同毫秒内 序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //毫秒内序列溢出 表示同一毫秒内序列号都已经被用完了
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                currentStmp = getNextMillis();
            }
        } else {
            //时间戳改变，毫秒内序列重置
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastStamp = currentStmp;

        //移位并通过或运算拼到一起组成64位的ID，得出十进制结果
        return (
                // 时间戳部分
                (currentStmp - START_STAMP) << TIMESTAMP_LEFT)
                // 数据中心部分
                | (dataCenterId << DATACENTER_LEFT)
                // 机器标识部分
                | (machineId << MACHINE_LEFT)
                // 序列号部分
                | sequence;
    }

    /**
     * 时钟回拨，获取下一个时钟
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @return 当前时间戳
     */
    protected long getNextMillis() {
        long currentMill = getNewTimeStmp();
        while (currentMill <= lastStamp) {
            currentMill = getNewTimeStmp();
        }
        return currentMill;
    }

    /**
     * 返回以毫秒为单位获取最新的系统时间
     *
     * @return 当前时间(毫秒)
     */
    protected long getNewTimeStmp() {
        return System.currentTimeMillis();
    }


    /**
     * 解析雪花原始算法结果
     *
     * @param idWorker 生成的id
     * @return java.lang.String
     * @date 2020/7/31 10:04
     */
    public String parseUID(long idWorker, String binaryCode) {

        BitsAllocator bitsAllocator = new BitsAllocator(41, 10, 12);

        long totalBits = BitsAllocator.TOTAL_BITS;
        long signBits = bitsAllocator.getSignBits();
        long timestampBits = bitsAllocator.getTimestampBits();
        long workerIdBits = bitsAllocator.getWorkerIdBits();
        long sequenceBits = bitsAllocator.getSequenceBits();

        // parse UID
        long sequence = (idWorker << (totalBits - sequenceBits)) >>> (totalBits - sequenceBits);
        long workerId = (idWorker << (timestampBits + signBits)) >>> (totalBits - workerIdBits);
        long deltaSeconds = idWorker >>> (workerIdBits + sequenceBits);

        Date thatTime = new Date(TimeUnit.MILLISECONDS.toMillis(START_STAMP + deltaSeconds));
        String thatTimeStr = DateUtils.formatByDateTimePattern(thatTime);

        // format as string
        return String.format("{\"binaryCode\":\"%s\",\"IdWorker\":\"%d\",\"timestamp\":\"%s\",\"dataCenterId\":\"%d\",\"machineId\":\"%d\",\"sequence\":\"%d\"}",
                binaryCode, idWorker, thatTimeStr, dataCenterId, machineId, sequence);
    }

    public static void main(String[] args) {
        SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);

        for (int i = 0; i < 1; i++) {
            long id = idWorker.nextId();
            /*System.out.println(Long.toBinaryString(id));
            System.out.println(id);*/
            System.out.println(idWorker.parseUID(id,Long.toBinaryString(id)));
        }
    }
}