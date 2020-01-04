package org.hazelcast.poc.service;


import com.hazelcast.flakeidgen.FlakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.security.SecureRandom;
import java.util.Random;

@Slf4j
@Service
public class IdGeneratorPocService {
    @Autowired
    private FlakeIdGenerator pocFlakeIdGenerator01;


    /**
     * 生成伪顺序长整数唯一序列，常用业务ID使用。
     */
    public long generate() {
        return pocFlakeIdGenerator01.newId();
    }

    // 0 -- 9, A -- Z
    //为了分辨度，    去掉了79:O, 73:I
    protected static final char[] CHAR_TABLE = new char[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69,
            70, 71, 72, 74, 75, 76, 77, 78, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90};
    protected static final int CHAR_TABLE_SIZE = CHAR_TABLE.length;

    /**
     * 生成伪乱序唯一代码，常用作业务代码、编码使用，同时考虑HBase等数据分区。
     */
    public String generateCode() {
        return generateCode(null);
    }

    public String generateCode(String prefix) {
        long number = this.generate();

        final StringBuilder sb = new StringBuilder();

        while (number > 0) {
            int mod = (int) (number % CHAR_TABLE_SIZE);
            sb.append(CHAR_TABLE[mod]);
            if (mod == 0) {
                mod = CHAR_TABLE_SIZE;
            }
            number = (number - mod) / CHAR_TABLE_SIZE;
        }

//        sb.reverse();
//        loopMove(sb);
//        loopMove(sb);
        if (prefix != null) {
            return prefix + sb.toString();
        } else {
            return sb.toString();
        }
    }

//    private void loopMove(StringBuilder sb) {
//        int pos = sb.length() - 1;
//        char ch = sb.charAt(pos);
//        sb.deleteCharAt(pos);
//        sb.insert(0, ch);
//    }

    /**
     * 生成随机HEX数，常用做Token。
     */
    private static final Random RANDOM = new SecureRandom();

    public String generateNonceToken() {
        final String code = this.generateCode() + RANDOM.nextLong();
        return DigestUtils.md5DigestAsHex(code.getBytes());
    }


}