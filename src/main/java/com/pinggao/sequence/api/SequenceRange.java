package com.pinggao.sequence.api;

public interface SequenceRange {
    /**
     * 获取序号范围内的下个id;
     *
     * @return
     */
    long nextValue();

    /**
     * 是否达到这个范围生成器id是否已经用尽
     *
     * @return
     */
    boolean over();
}
